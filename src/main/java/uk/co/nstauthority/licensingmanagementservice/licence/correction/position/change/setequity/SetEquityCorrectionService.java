package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;

@Service
public class SetEquityCorrectionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public SetEquityCorrectionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public List<SetEquityOperation> getCommittedSetEquityOperations(
      LicencePositionCorrection licencePositionCorrection
  ) {
    return setEquityOperations(licencePositionCorrection.getPayload().changes());
  }

  public List<SetEquityRow> getSetEquityViews(List<SetEquityOperation> operations) {
    var organisationIds = operations.stream()
        .map(SetEquityOperation::transferTo)
        .toList();

    var organisationNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationIds);

    return operations.stream()
        .map(operation -> new SetEquityRow(
            organisationNames.getOrDefault(operation.transferTo(), ""),
            operation.equity()
        ))
        .toList();
  }

  @Transactional
  public void commitSetEquity(
      LicencePositionCorrection licencePositionCorrection,
      List<SetEquityOperation> operations
  ) {
    applySetEquity(licencePositionCorrection, operations);
  }

  public List<SetEquityOperation> getCommittedSetEquityOperationsForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .map(this::getCommittedSetEquityOperations)
        .orElseGet(List::of);
  }

  @Transactional
  public void commitSetEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      List<SetEquityOperation> operations
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    applySetEquity(positionCorrection, operations);
  }

  private void applySetEquity(
      LicencePositionCorrection licencePositionCorrection,
      List<SetEquityOperation> operations
  ) {
    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection, SetEquityOperation.class, operations);
  }

  private List<SetEquityOperation> setEquityOperations(List<LicencePositionChangeType> changes) {
    return licencePositionCorrectionService.getAddOperationsOfType(changes, SetEquityOperation.class);
  }
}