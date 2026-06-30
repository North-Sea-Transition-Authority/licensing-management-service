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
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.operations.LicencePositionChangeOperation;

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

    var currentChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(currentLicencePosition)
        .withOperations(List.of(
            LicencePositionChangeOperation.newAdministratorChange().withOperator(CURRENT_ADMIN_ID).build()))
        .build();

    when(organisationUnitQueryService.getOrganisationUnitNameById(CURRENT_ADMIN_ID))
        .thenReturn(Optional.of(CURRENT_ADMIN_NAME));

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition,
        List.of(currentLicencePosition),
        List.of(currentChange)
    );

    assertThat(result.administratorStateView().organisationName()).isEqualTo(CURRENT_ADMIN_NAME);
  }

  @Test
  void buildAdministratorState_changeBeforeCurrentPosition() {
    var oldestLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var gapAfterMiddleLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var middleLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var oldestChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(oldestLicencePosition)
        .withOperations(List.of(
            LicencePositionChangeOperation.newAdministratorChange().withOperator(OLDER_ADMIN_ID).build()))
        .build();

    var middleChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(middleLicencePosition)
        .withOperations(List.of(
            LicencePositionChangeOperation.newAdministratorChange().withOperator(MOST_RECENT_ADMIN_ID).build()))
        .build();

    when(organisationUnitQueryService.getOrganisationUnitNameById(MOST_RECENT_ADMIN_ID))
        .thenReturn(Optional.of(MOST_RECENT_ADMIN_NAME));

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition,
        List.of(oldestLicencePosition, middleLicencePosition, gapAfterMiddleLicencePosition, currentLicencePosition),
        List.of(oldestChange, middleChange)
    );

    assertThat(result.administratorStateView().organisationName()).isEqualTo(MOST_RECENT_ADMIN_NAME);
  }

  @Test
  void buildAdministratorState_adminIdNotFound() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(currentLicencePosition)
        .withOperations(List.of(
            LicencePositionChangeOperation.newAdministratorChange().withOperator(CURRENT_ADMIN_ID).build()))
        .build();

    when(organisationUnitQueryService.getOrganisationUnitNameById(CURRENT_ADMIN_ID))
        .thenReturn(Optional.empty());

    var result = licencePositionStateViewService.getStateView(
        currentLicencePosition,
        List.of(currentLicencePosition),
        List.of(currentChange)
    );

    assertThat(result.administratorStateView().organisationName()).isEmpty();
  }
}