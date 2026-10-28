package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.SetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.TransferEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.EquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.EquitySetterOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Service
public class EquityChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final TransferEquityCorrectionService transferEquityCorrectionService;
  private final SetEquityCorrectionService setEquityCorrectionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final FeatureService featureService;

  public EquityChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      TransferEquityCorrectionService transferEquityCorrectionService,
      SetEquityCorrectionService setEquityCorrectionService,
      LicencePositionChangeService licencePositionChangeService,
      FeatureService featureService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.transferEquityCorrectionService = transferEquityCorrectionService;
    this.setEquityCorrectionService = setEquityCorrectionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.featureService = featureService;
  }

  @Transactional
  public void removeExistingEquityChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String changeId
  ) {
    licencePositionCorrectionService.stageRemovalOfExecutedChange(licenceCorrection, licencePosition, changeId);
  }

  @Transactional
  public void undoEquityChange(LicenceCorrection licenceCorrection, String changeId) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);

    var changeToUndo = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId);

    var containsEquityOperation = licencePositionCorrectionService.resolveStagedChangeOperations(changeToUndo)
        .stream()
        .anyMatch(EquityOperation.class::isInstance);

    if (!containsEquityOperation) {
      throw new IllegalStateException("Change %s does not contain an equity operation".formatted(changeId));
    }

    licencePositionCorrectionService.dropStagedChange(positionCorrection, changeId);
  }

  public EquityChangeContext getExecutedEquityChangeContext(String changeId) {
    var liveChange = licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId));
    return buildContext(LicencePositionChange.operationsOf(liveChange));
  }

  public EquityChangeContext getEquityChangeContext(LicenceCorrection licenceCorrection, String changeId) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);

    return buildContext(licencePositionCorrectionService.resolveStagedChangeOperations(
        licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId)));
  }

  private EquityChangeContext buildContext(List<LicenceOperation> operations) {
    var equitySetterOperations = operations.stream()
        .filter(EquitySetterOperation.class::isInstance)
        .map(EquitySetterOperation.class::cast)
        .toList();

    var setEquityRows = setEquityCorrectionService.getSetEquityViews(equitySetterOperations);

    var transferEquityOperations = operations.stream()
        .filter(TransferEquityOperation.class::isInstance)
        .map(TransferEquityOperation.class::cast)
        .toList();

    var transferEquityRows = transferEquityCorrectionService.getTransferEquityViews(transferEquityOperations);

    return new EquityChangeContext(setEquityRows, transferEquityRows, subareaLabel(operations));
  }

  /**
   * The label of the subarea where equity was changed.
   *
   * @return null when there is no subarea to name
   */
  @Nullable
  private String subareaLabel(List<LicenceOperation> operations) {
    var subareaSetEquityOperation = operations.stream()
        .filter(SubareaSetEquityOperation.class::isInstance)
        .map(SubareaSetEquityOperation.class::cast)
        .findFirst()
        .orElse(null);

    if (subareaSetEquityOperation == null) {
      return null;
    }

    var featuresById = featureService.getFeaturesByIds(List.of(subareaSetEquityOperation.blockFeatureId()))
        .stream()
        .collect(Collectors.toMap(Feature::getId, Feature::getFeatureName));

    return subareaSetEquityOperation.subareaLabel(featuresById);
  }
}