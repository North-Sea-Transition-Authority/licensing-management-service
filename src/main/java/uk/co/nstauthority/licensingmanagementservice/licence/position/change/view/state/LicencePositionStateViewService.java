package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionAdministratorChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;

@Service
public class LicencePositionStateViewService {

  public LicencePositionStateView getStateView(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions,
      Map<Integer, String> organisationNames
  ) {
    return new LicencePositionStateView(
        buildAdministratorState(currentLicencePositionId, chronologicalPositions, organisationNames)
    );
  }

  private AdministratorStateView buildAdministratorState(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions,
      Map<Integer, String> organisationNames
  ) {
    var currentAdminId =
        LicencePositionAdministratorChangeUtil.resolveCurrentAdministratorId(currentLicencePositionId, chronologicalPositions);

    if (currentAdminId == null) {
      return new AdministratorStateView("");
    }

    var administratorName = organisationNames.getOrDefault(currentAdminId, "");

    return new AdministratorStateView(administratorName);
  }

}
