package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.EquitySetterOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;

@Service
public class SetEquityCorrectionService {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  SetEquityCorrectionService(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  List<SetEquityOperation> getCommittedSetEquityOperations(LicencePositionStagedPayload licencePositionStagedPayload) {
    var changes = licencePositionStagedPayload.getPayload().changes();
    return licencePositionStagedPayloadService.getAddOperationsOfType(changes, SetEquityOperation.class);
  }

  public List<SetEquityRow> getSetEquityViews(List<? extends EquitySetterOperation> equityOperations) {
    var organisationIds = equityOperations.stream()
        .map(EquitySetterOperation::transferTo)
        .toList();

    var organisationNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationIds);

    return equityOperations.stream()
        .map(operation -> new SetEquityRow(
            organisationNames.getOrDefault(operation.transferTo(), ""),
            operation.equity()
        ))
        .toList();
  }

  public List<SetEquityOperation> getCommittedSetEquityOperations(CorrectionPosition correctionPosition) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed(LicenceCorrection correction, LicencePosition licencePosition) ->
          getCommittedSetEquityOperationsForExecutedPosition(correction, licencePosition);
      case CorrectionPosition.Staged(LicencePositionStagedPayload licencePositionStagedPayload) ->
          getCommittedSetEquityOperations(licencePositionStagedPayload);
    };
  }

  public void commitSetEquity(CorrectionPosition correctionPosition, List<SetEquityOperation> operations) {
    switch (correctionPosition) {
      case CorrectionPosition.Executed(LicenceCorrection correction, LicencePosition licencePosition) ->
          commitSetEquityForExecutedPosition(correction, licencePosition, operations);
      case CorrectionPosition.Staged(LicencePositionStagedPayload licencePositionStagedPayload) ->
          commitSetEquity(licencePositionStagedPayload, operations);
    }
  }

  private List<SetEquityOperation> getCommittedSetEquityOperationsForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return licencePositionStagedPayloadService.findUpdatePositionStagedPayload(licenceCorrection, licencePosition)
        .map(this::getCommittedSetEquityOperations)
        .orElse(List.of());
  }

  void commitSetEquity(LicencePositionStagedPayload licencePositionStagedPayload, List<SetEquityOperation> operations) {
    licencePositionStagedPayloadService.replaceAddChangeFor(licencePositionStagedPayload, SetEquityOperation.class, operations);
  }

  private void commitSetEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      List<SetEquityOperation> operations
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition);

    licencePositionStagedPayloadService.replaceAddChangeFor(positionStagedPayload, SetEquityOperation.class, operations);
  }
}
