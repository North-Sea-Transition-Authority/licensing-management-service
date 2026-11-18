package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

@Service
public class SubareaSetEquityCorrectionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  SubareaSetEquityCorrectionService(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  List<SubareaSetEquityOperation> getCommittedSubareaSetEquityOperations(
      LicencePositionCorrection licencePositionCorrection,
      SubareaOption subareaOption
  ) {
    return licencePositionCorrectionService
        .getAddOperationsOfType(licencePositionCorrection.getPayload().changes(), SubareaSetEquityOperation.class)
        .stream()
        .filter(isForSubarea(subareaOption.key()))
        .toList();
  }

  List<SubareaSetEquityOperation> getCommittedSubareaSetEquityOperations(
      CorrectionPosition correctionPosition,
      SubareaOption subareaOption
  ) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed executed -> getCommittedSubareaSetEquityOperationsForExecutedPosition(
          executed.correction(),
          executed.licencePosition(),
          subareaOption
      );
      case CorrectionPosition.Staged staged ->
          getCommittedSubareaSetEquityOperations(staged.licencePositionCorrection(), subareaOption);
    };
  }

  @Transactional
  void commitSubareaSetEquity(
      CorrectionPosition correctionPosition,
      SubareaOption subareaOption,
      List<SubareaSetEquityOperation> operations
  ) {
    switch (correctionPosition) {
      case CorrectionPosition.Executed executed -> commitSubareaSetEquityForExecutedPosition(
          executed.correction(),
          executed.licencePosition(),
          subareaOption,
          operations
      );
      case CorrectionPosition.Staged staged ->
          commitSubareaSetEquity(staged.licencePositionCorrection(), subareaOption, operations);
    }
  }

  private List<SubareaSetEquityOperation> getCommittedSubareaSetEquityOperationsForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      SubareaOption subareaOption
  ) {
    return licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .map(positionCorrection -> getCommittedSubareaSetEquityOperations(positionCorrection, subareaOption))
        .orElse(List.of());
  }

  @Transactional
  void commitSubareaSetEquity(
      LicencePositionCorrection licencePositionCorrection,
      SubareaOption subareaOption,
      List<SubareaSetEquityOperation> operations
  ) {
    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
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
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    // the subarea may have been created by a live change, so the equity is ordered after the live changes too
    licencePositionCorrectionService.replaceAddChangeForExecutedPosition(
        licenceCorrection,
        licencePosition,
        positionCorrection,
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
      LicencePositionCorrection positionCorrection,
      LicencePositionState state,
      UUID blockFeatureId,
      List<SubareaTransfer> transfers
  ) {
    var stagedOperations = licencePositionCorrectionService
        .getAddOperationsOfType(positionCorrection.getPayload().changes(), SubareaSetEquityOperation.class);

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

    var targetLicencePosition = positionCorrection.getTargetLicencePosition();

    if (targetLicencePosition == null) {
      licencePositionCorrectionService.addChanges(positionCorrection, subareaEquityOperationsPerSubarea);
    } else {
      licencePositionCorrectionService.addChangesForExecutedPosition(
          positionCorrection.getLicenceCorrection(),
          targetLicencePosition,
          positionCorrection,
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