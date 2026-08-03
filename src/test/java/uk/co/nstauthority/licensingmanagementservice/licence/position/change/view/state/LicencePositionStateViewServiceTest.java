package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;

class LicencePositionStateViewServiceTest {

  private static final int CURRENT_ADMIN_ID = 100;
  private static final int OLDER_ADMIN_ID = 200;
  private static final int MOST_RECENT_ADMIN_ID = 300;
  private static final String CURRENT_ADMIN_NAME = "Current Admin Ltd";
  private static final String MOST_RECENT_ADMIN_NAME = "Most Recent Admin Ltd";

  private final LicencePositionStateViewService licencePositionStateViewService = new LicencePositionStateViewService();

  @Test
  void buildAdministratorState_changeOnCurrentPosition() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(CURRENT_ADMIN_ID).build()
    );

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition.getId(),
        List.of(currentChronologicalPosition),
        Map.of(CURRENT_ADMIN_ID, CURRENT_ADMIN_NAME)
    );

    assertThat(result.administratorStateView().organisationName()).isEqualTo(CURRENT_ADMIN_NAME);
  }

  @Test
  void buildAdministratorState_changeBeforeCurrentPosition() {
    var oldestLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var middleLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var gapAfterMiddleLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var oldestChronologicalPosition = ChronologicalPositionTestUtil.live(
        oldestLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(OLDER_ADMIN_ID).build()
    );
    var middleChronologicalPosition = ChronologicalPositionTestUtil.live(
        middleLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(MOST_RECENT_ADMIN_ID).build()
    );
    var gapAfterMiddleChronologicalPosition = ChronologicalPositionTestUtil.live(gapAfterMiddleLicencePosition);
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(currentLicencePosition);

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition.getId(),
        List.of(
            oldestChronologicalPosition,
            middleChronologicalPosition,
            gapAfterMiddleChronologicalPosition,
            currentChronologicalPosition
        ),
        Map.of(OLDER_ADMIN_ID, "Older Admin Ltd", MOST_RECENT_ADMIN_ID, MOST_RECENT_ADMIN_NAME)
    );

    assertThat(result.administratorStateView().organisationName()).isEqualTo(MOST_RECENT_ADMIN_NAME);
  }

  @Test
  void buildAdministratorState_adminIdNotFound() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(CURRENT_ADMIN_ID).build()
    );

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition.getId(),
        List.of(currentChronologicalPosition),
        Map.of()
    );

    assertThat(result.administratorStateView().organisationName()).isEmpty();
  }

  @Test
  void getStateView_whenNoAdminChange_returnsEmptyAdministratorStateWithoutFailing() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(currentLicencePosition);

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition.getId(),
        List.of(currentChronologicalPosition),
        Map.of()
    );

    assertThat(result.administratorStateView().organisationName()).isEmpty();
  }


}