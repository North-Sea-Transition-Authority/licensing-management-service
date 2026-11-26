package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;

@Service
public class SubareaSetEquityCorrectionService {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  SubareaSetEquityCorrectionService(LicencePositionStagedPayloadService licencePositionStagedPayloadService) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
  }

  List<SubareaSetEquityOperation> getCommittedSubareaSetEquityOperations(
      LicencePositionStagedPayload licencePositionStagedPayload,
      SubareaOption subareaOption
  ) {
    return licencePositionStagedPayloadService
        .getAddOperationsOfType(licencePositionStagedPayload.getPayload().changes(), SubareaSetEquityOperation.class)
        .stream()
        .filter(isForSubarea(subareaOption.key()))
        .toList();
  }

  List<SubareaSetEquityOperation> getCommittedSubareaSetEquityOperations(
      CorrectionPosition correctionPosition,
      SubareaOption subareaOption
  ) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed(LicenceCorrection correction, LicencePosition licencePosition) ->
          getCommittedSubareaSetEquityOperationsForExecutedPosition(correction, licencePosition, subareaOption);
      case CorrectionPosition.Staged(LicencePositionStagedPayload licencePositionStagedPayload) ->
          getCommittedSubareaSetEquityOperations(licencePositionStagedPayload, subareaOption);
    };
  }

  @Transactional
  void commitSubareaSetEquity(
      CorrectionPosition correctionPosition,
      SubareaOption subareaOption,
      List<SubareaSetEquityOperation> operations
  ) {
    switch (correctionPosition) {
      case CorrectionPosition.Executed(LicenceCorrection correction, LicencePosition licencePosition) ->
          commitSubareaSetEquityForExecutedPosition(correction, licencePosition, subareaOption, operations);
      case CorrectionPosition.Staged(LicencePositionStagedPayload licencePositionStagedPayload) ->
          commitSubareaSetEquity(licencePositionStagedPayload, subareaOption, operations);
    }
  }

  private List<SubareaSetEquityOperation> getCommittedSubareaSetEquityOperationsForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      SubareaOption subareaOption
  ) {
    return licencePositionStagedPayloadService.findUpdatePositionStagedPayload(licenceCorrection, licencePosition)
        .map(positionStagedPayload -> getCommittedSubareaSetEquityOperations(positionStagedPayload, subareaOption))
        .orElse(List.of());
  }

  @Transactional
  void commitSubareaSetEquity(
      LicencePositionStagedPayload licencePositionStagedPayload,
      SubareaOption subareaOption,
      List<SubareaSetEquityOperation> operations
  ) {
    licencePositionStagedPayloadService.replaceAddChangeFor(
        licencePositionStagedPayload,
        SubareaSetEquityOperation.class,
        isForSubarea(subareaOption.key()),
        withCurrentSubarea(operations, subareaOption)
    );
  }

  private void commitSubareaSetEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      SubareaOption subareaOption,
      List<SubareaSetEquityOperation> operations
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition);

    // the subarea may have been created by a live change, so the equity is ordered after the live changes too
    licencePositionStagedPayloadService.replaceAddChangeForExecutedPosition(
        licenceCorrection,
        licencePosition,
        positionStagedPayload,
        SubareaSetEquityOperation.class,
        isForSubarea(subareaOption.key()),
        withCurrentSubarea(operations, subareaOption)
    );
  }

  /**
   * Stages the beneficial interests of each transfer's replaced subareas on each of its output subareas, unless the
   * output's lineage is lost or beneficial interests are already staged on it.
   */
  @Transactional
  public void cascadeSubareaEquity(
      LicencePositionStagedPayload positionStagedPayload,
      LicencePositionState state,
      UUID blockFeatureId,
      List<SubareaTransfer> transfers
  ) {
    var stagedOperations = licencePositionStagedPayloadService
        .getAddOperationsOfType(positionStagedPayload.getPayload().changes(), SubareaSetEquityOperation.class);

    List<List<SubareaSetEquityOperation>> subareaEquityOperationsPerSubarea = transfers
        .stream()
        .filter(transfer -> !isLineageLost(state, blockFeatureId, transfer.replacedSubareas()))
        .flatMap(transfer -> transfer.outputSubareas().stream()
            .filter(outputSubarea -> stagedOperations.stream()
                .noneMatch(isForSubarea(outputSubarea.keyForBlockFeatureId(blockFeatureId)))
            )
            .map(
                outputSubarea -> toNewSubareaEquityOperations(state, blockFeatureId, transfer.replacedSubareas(), outputSubarea))
        )
        .toList();

    if (subareaEquityOperationsPerSubarea.isEmpty()) {
      return;
    }

    var targetLicencePosition = positionStagedPayload.getTargetLicencePosition();

    if (targetLicencePosition == null) {
      licencePositionStagedPayloadService.addChanges(positionStagedPayload, subareaEquityOperationsPerSubarea);
    } else {
      licencePositionStagedPayloadService.addChangesForExecutedPosition(
          positionStagedPayload.getLicenceCorrection(),
          targetLicencePosition,
          positionStagedPayload,
          subareaEquityOperationsPerSubarea
      );
    }
  }

  private static List<SubareaSetEquityOperation> toNewSubareaEquityOperations(
      LicencePositionState state,
      UUID blockFeatureId,
      List<SubareaDetails> originalSubareas,
      SubareaDetails newSubarea
  ) {
    return state
        .subareaEquityOf(blockFeatureId, originalSubareas.getFirst())
        .entrySet().stream()
        .map(equity -> LicenceOperation.newSubareaSetEquityOperation()
            .withBlockFeatureId(blockFeatureId)
            .withSubarea(newSubarea)
            .withTransferTo(equity.getKey())
            .withEquity(equity.getValue())
            .build())
        .toList();
  }

  /**
   * Lineage is lost when the subareas a subarea came from do not all share the same beneficial interests, or when it
   * came from none of them.
   */
  public boolean isLineageLost(
      LicencePositionState state,
      UUID blockFeatureId,
      Collection<SubareaDetails> originalSubareas
  ) {
    return originalSubareas.stream()
        .map(subarea -> state.subareaEquityOf(blockFeatureId, subarea))
        .distinct()
        .count() != 1;
  }

  private static Predicate<SubareaSetEquityOperation> isForSubarea(String subareaKey) {
    return operation -> operation.subareaKey().equals(subareaKey);
  }

  private static List<SubareaSetEquityOperation> withCurrentSubarea(
      List<SubareaSetEquityOperation> operations,
      SubareaOption subareaOption
  ) {
    return operations.stream()
        .map(operation -> operation.withSubarea(subareaOption.subarea()))
        .toList();
  }
}
