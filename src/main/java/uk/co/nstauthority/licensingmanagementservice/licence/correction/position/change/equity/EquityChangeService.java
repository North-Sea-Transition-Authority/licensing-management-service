package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.SetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.TransferEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.EquitySetterOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureSummary;

@Service
public class EquityChangeService {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final TransferEquityCorrectionService transferEquityCorrectionService;
  private final SetEquityCorrectionService setEquityCorrectionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final FeatureService featureService;

  public EquityChangeService(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      TransferEquityCorrectionService transferEquityCorrectionService,
      SetEquityCorrectionService setEquityCorrectionService,
      LicencePositionChangeService licencePositionChangeService,
      FeatureService featureService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.transferEquityCorrectionService = transferEquityCorrectionService;
    this.setEquityCorrectionService = setEquityCorrectionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.featureService = featureService;
  }

  public EquityChangeContext getExecutedEquityChangeContext(String changeId) {
    var liveChange = licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId));
    return buildContext(LicencePositionChange.operationsOf(liveChange));
  }

  public EquityChangeContext getEquityChangeContext(LicenceCorrection licenceCorrection, String changeId) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .getPositionStagedPayloadContainingChange(licenceCorrection, changeId);

    return buildContext(licencePositionStagedPayloadService.resolveStagedChangeOperations(
        licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, changeId)));
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
        .collect(Collectors.toMap(Feature::getId, FeatureSummary::from));

    return subareaSetEquityOperation.subareaLabel(featuresById);
  }
}