package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

class LicencePositionStateViewResolverTest {

  private static final int CURRENT_ADMIN_ID = 100;
  private static final String CURRENT_ADMIN_NAME = "Current Admin Ltd";

  @Test
  void getStateView_resolvesAdministratorNameFromState() {
    var currentPositionId = UUID.randomUUID();

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        Map.of(currentPositionId, new LicencePositionState(CURRENT_ADMIN_ID)),
        Map.of(CURRENT_ADMIN_ID, CURRENT_ADMIN_NAME)
    );

    assertThat(result.administratorStateView().organisationName()).isEqualTo(CURRENT_ADMIN_NAME);
  }

  @Test
  void getStateView_whenNoStateForPosition_returnsEmptyName() {
    var currentPositionId = UUID.randomUUID();

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        Map.of(),
        Map.of(CURRENT_ADMIN_ID, CURRENT_ADMIN_NAME)
    );

    assertThat(result.administratorStateView().organisationName()).isEmpty();
  }

  @Test
  void getStateView_whenAdministratorNameNotFound_returnsEmptyName() {
    var currentPositionId = UUID.randomUUID();

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        Map.of(currentPositionId, new LicencePositionState(CURRENT_ADMIN_ID)),
        Map.of()
    );

    assertThat(result.administratorStateView().organisationName()).isEmpty();
  }
}
