package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;

@Service
public class LicencePositionStateViewService {

  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionStateViewService(
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public LicencePositionStateView getStateView(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    return new LicencePositionStateView(
        buildAdministratorState(currentLicencePositionId, chronologicalPositions)
    );
  }

  /**
   * Resolves the administrator of the given position by walking the chronological positions and applying each
   * administrator change up to and including the current position. As the changes are folded into the
   * {@link ChronologicalPosition}s, any in-progress correction is taken into account.
   */
  public Integer resolveCurrentAdministratorId(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    return resolveAdministratorId(currentLicencePositionId, chronologicalPositions, true);
  }

  /**
   * Resolves the administrator entering the given position, i.e. the administrator in place immediately before the
   * position's own change is applied (the administrator being replaced when correcting that position's change).
   */
  public Integer resolvePreviousAdministratorId(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    return resolveAdministratorId(currentLicencePositionId, chronologicalPositions, false);
  }

  private Integer resolveAdministratorId(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions,
      boolean inclusive
  ) {
    var administratorIdChangeByPositionId = LicencePositionChangeUtil.administratorIdChangeByPositionId(chronologicalPositions);

    Integer administratorId = null;
    for (var chronologicalPosition : chronologicalPositions) {
      var isCurrentPosition = chronologicalPosition.id().equals(currentLicencePositionId);
      if (inclusive || !isCurrentPosition) {
        var operatorId = administratorIdChangeByPositionId.get(chronologicalPosition.id());
        if (operatorId != null) {
          administratorId = operatorId;
        }
      }
      if (isCurrentPosition) {
        break;
      }
    }

    return administratorId;
  }

  private AdministratorStateView buildAdministratorState(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    var currentAdminId = resolveCurrentAdministratorId(currentLicencePositionId, chronologicalPositions);

    if (currentAdminId == null) {
      return new AdministratorStateView("");
    }

    var administratorName = organisationUnitQueryService.getOrganisationUnitNameById(currentAdminId).orElse("");

    return new AdministratorStateView(administratorName);
  }

}
