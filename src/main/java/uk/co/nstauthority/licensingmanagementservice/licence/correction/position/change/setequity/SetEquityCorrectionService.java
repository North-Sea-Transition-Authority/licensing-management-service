package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.EquitySetterOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;

@Service
public class SetEquityCorrectionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  SetEquityCorrectionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public List<SetEquityOperation> getCommittedSetEquityOperations(LicencePositionCorrection licencePositionCorrection) {
    var changes = licencePositionCorrection.getPayload().changes();
    return licencePositionCorrectionService.getAddOperationsOfType(changes, SetEquityOperation.class);
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

  public List<SetEquityOperation> getCommittedSetEquityOperationsForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .map(this::getCommittedSetEquityOperations)
        .orElse(List.of());
  }

  public void commitSetEquity(
      LicencePositionCorrection licencePositionCorrection,
      List<SetEquityOperation> operations
  ) {
    licencePositionCorrectionService.replaceAddChangeFor(licencePositionCorrection, SetEquityOperation.class, operations);
  }

  public void commitSetEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      List<SetEquityOperation> operations
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    licencePositionCorrectionService.replaceAddChangeFor(positionCorrection, SetEquityOperation.class, operations);
  }
}