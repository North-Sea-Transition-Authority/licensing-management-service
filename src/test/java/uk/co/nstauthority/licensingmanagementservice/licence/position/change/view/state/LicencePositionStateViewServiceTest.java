package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionStateViewServiceTest {

  private static final int CURRENT_ADMIN_ID = 100;
  private static final int OLDER_ADMIN_ID = 200;
  private static final int MOST_RECENT_ADMIN_ID = 300;
  private static final String CURRENT_ADMIN_NAME = "Current Admin Ltd";
  private static final String MOST_RECENT_ADMIN_NAME = "Most Recent Admin Ltd";

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private LicencePositionStateViewService licencePositionStateViewService;

  @Test
  void buildAdministratorState_changeOnCurrentPosition() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(CURRENT_ADMIN_ID).build()
    );

    when(organisationUnitQueryService.getOrganisationUnitNameById(CURRENT_ADMIN_ID))
        .thenReturn(Optional.of(CURRENT_ADMIN_NAME));

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition.getId(),
        List.of(currentChronologicalPosition)
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

    when(organisationUnitQueryService.getOrganisationUnitNameById(MOST_RECENT_ADMIN_ID))
        .thenReturn(Optional.of(MOST_RECENT_ADMIN_NAME));

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition.getId(),
        List.of(
            oldestChronologicalPosition,
            middleChronologicalPosition,
            gapAfterMiddleChronologicalPosition,
            currentChronologicalPosition
        )
    );

    assertThat(result.administratorStateView().organisationName()).isEqualTo(MOST_RECENT_ADMIN_NAME);
  }

  @Test
  void resolveCurrentAdministratorId_returnsMostRecentChangeUpToCurrentPosition() {
    var oldestLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var middleLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var laterLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var oldestChronologicalPosition = ChronologicalPositionTestUtil.live(
        oldestLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(OLDER_ADMIN_ID).build()
    );
    var middleChronologicalPosition = ChronologicalPositionTestUtil.live(
        middleLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(CURRENT_ADMIN_ID).build()
    );
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(currentLicencePosition);
    // a change on a position after the current one must be ignored
    var laterChronologicalPosition = ChronologicalPositionTestUtil.live(
        laterLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(MOST_RECENT_ADMIN_ID).build()
    );

    var result = licencePositionStateViewService.resolveCurrentAdministratorId(
        currentLicencePosition.getId(),
        List.of(
            oldestChronologicalPosition,
            middleChronologicalPosition,
            currentChronologicalPosition,
            laterChronologicalPosition
        )
    );

    assertThat(result).isEqualTo(CURRENT_ADMIN_ID);
  }

  @Test
  void resolveCurrentAdministratorId_whenNoAdminChange_returnsNull() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(currentLicencePosition);

    var result = licencePositionStateViewService.resolveCurrentAdministratorId(
        currentLicencePosition.getId(),
        List.of(currentChronologicalPosition)
    );

    assertThat(result).isNull();
  }

  @Test
  void buildAdministratorState_adminIdNotFound() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(CURRENT_ADMIN_ID).build()
    );

    when(organisationUnitQueryService.getOrganisationUnitNameById(CURRENT_ADMIN_ID))
        .thenReturn(Optional.empty());

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition.getId(),
        List.of(currentChronologicalPosition)
    );

    assertThat(result.administratorStateView().organisationName()).isEmpty();
  }
}