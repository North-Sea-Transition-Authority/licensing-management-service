package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeUtil;

@Service
public class LicencePositionStateViewService {

  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionStateViewService(
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public LicencePositionStateView getStateView(
      LicencePosition currentLicencePosition,
      List<LicencePosition> chronologicalLicencePositions,
      List<LicencePositionChange> licencePositionChanges
  ) {
    return new LicencePositionStateView(
        buildAdministratorState(currentLicencePosition, chronologicalLicencePositions, licencePositionChanges)
    );
  }

  public Integer resolveCurrentAdministratorId(
      LicencePosition currentLicencePosition,
      List<LicencePosition> chronologicalLicencePositions,
      List<LicencePositionChange> licencePositionChanges
  ) {
    var administratorIdChangeByPositionId = LicencePositionChangeUtil.administratorIdChangeByPositionId(licencePositionChanges);

    Integer currentAdminId = null;
    for (var licencePosition : chronologicalLicencePositions) {
      var administratorId = administratorIdChangeByPositionId.get(licencePosition.getId());
      if (administratorId != null) {
        currentAdminId = administratorId;
      }
      if (licencePosition.getId().equals(currentLicencePosition.getId())) {
        break;
      }
    }

    return currentAdminId;
  }

  private AdministratorStateView buildAdministratorState(
      LicencePosition currentLicencePosition,
      List<LicencePosition> chronologicalLicencePositions,
      List<LicencePositionChange> licencePositionChanges
  ) {

    var currentAdminId = resolveCurrentAdministratorId(
        currentLicencePosition, chronologicalLicencePositions, licencePositionChanges);

    if (currentAdminId == null) {
      throw new IllegalStateException(
          "No administrator for current position {%s}, expected an administrator change on the earliest position"
              .formatted(currentLicencePosition.getId())
      );
    }

    var administratorName = organisationUnitQueryService.getOrganisationUnitNameById(currentAdminId).orElse("");

    return new AdministratorStateView(administratorName);
  }

}
