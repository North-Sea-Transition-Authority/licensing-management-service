package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;

public final class LicencePositionStateViewResolver {

  private LicencePositionStateViewResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static LicencePositionStateView getStateView(
      UUID currentLicencePositionId,
      Map<UUID, LicencePositionState> statesByChronologicalPositionId,
      Map<Integer, String> organisationNames
  ) {
    var state = statesByChronologicalPositionId.getOrDefault(currentLicencePositionId, LicencePositionState.EMPTY);

    return new LicencePositionStateView(
        buildAdministratorState(state.administratorId(), organisationNames)
    );
  }

  private static AdministratorStateView buildAdministratorState(
      @Nullable Integer currentAdministratorId,
      Map<Integer, String> organisationNames
  ) {
    if (currentAdministratorId == null) {
      return new AdministratorStateView("");
    }

    return new AdministratorStateView(organisationNames.getOrDefault(currentAdministratorId, ""));
  }
}
