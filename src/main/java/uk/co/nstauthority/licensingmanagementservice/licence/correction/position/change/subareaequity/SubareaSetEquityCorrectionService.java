package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

@Service
public class SubareaSetEquityCorrectionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  SubareaSetEquityCorrectionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
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

  List<SubareaSetEquityOperation> getCommittedSubareaSetEquityOperationsForExecutedPosition(
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

  @Transactional
  void commitSubareaSetEquityForExecutedPosition(
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
   * Stages the beneficial interests of the original subareas on the new subarea, unless its lineage is lost or
   * beneficial interests are already staged on it.
   */
  @Transactional
  public void cascadeSubareaEquity(
      LicencePositionCorrection positionCorrection,
      LicencePositionState state,
      UUID blockFeatureId,
      Collection<SubareaDetails> originalSubareas,
      SubareaDetails newSubarea
  ) {
    var isForNewSubarea = isForSubarea(newSubarea.keyForBlockFeatureId(blockFeatureId));

    if (isLineageLost(state, blockFeatureId, originalSubareas)
        || hasStagedSubareaSetEquity(positionCorrection, isForNewSubarea)) {
      return;
    }

    var operations = state
        .subareaEquityOf(blockFeatureId, originalSubareas.iterator().next())
        .entrySet().stream()
        .map(equity -> LicenceOperation.newSubareaSetEquityOperation()
            .withBlockFeatureId(blockFeatureId)
            .withSubarea(newSubarea)
            .withTransferTo(equity.getKey())
            .withEquity(equity.getValue())
            .build())
        .toList();
    var targetLicencePosition = positionCorrection.getTargetLicencePosition();

    if (targetLicencePosition == null) {
      licencePositionCorrectionService.replaceAddChangeFor(
          positionCorrection,
          SubareaSetEquityOperation.class,
          isForNewSubarea,
          operations
      );
    } else {
      licencePositionCorrectionService.replaceAddChangeForExecutedPosition(
          positionCorrection.getLicenceCorrection(),
          targetLicencePosition,
          positionCorrection,
          SubareaSetEquityOperation.class,
          isForNewSubarea,
          operations
      );
    }
  }

  /**
   * Lineage is lost when the subareas a subarea came from do not all share the same beneficial interests, or when it
   * came from none of them.
   */
  boolean isLineageLost(
      LicencePositionState state,
      UUID blockFeatureId,
      Collection<SubareaDetails> originalSubareas
  ) {
    return originalSubareas.stream()
        .map(subarea -> state.subareaEquityOf(blockFeatureId, subarea))
        .distinct()
        .count() != 1;
  }

  private boolean hasStagedSubareaSetEquity(
      LicencePositionCorrection positionCorrection,
      Predicate<SubareaSetEquityOperation> isForSubarea
  ) {
    return licencePositionCorrectionService
        .getAddOperationsOfType(positionCorrection.getPayload().changes(), SubareaSetEquityOperation.class)
        .stream()
        .anyMatch(isForSubarea);
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