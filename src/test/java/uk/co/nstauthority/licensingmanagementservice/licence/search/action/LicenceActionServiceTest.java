package uk.co.nstauthority.licensingmanagementservice.licence.search.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

@ExtendWith(MockitoExtension.class)
class LicenceActionServiceTest {

  @Mock
  private TeamQueryService TeamQueryService;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @InjectMocks
  private LicenceActionService licenceActionService;

  @Test
  void getAvailableUserActionItems_licenceManagedByLms() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    assertThat(licenceActionService.getAvailableUserActionItems(licence, null))
        .contains(LicenceActionItem.MANAGE_LICENSEES.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceNotManagedByLms() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    assertThat(licenceActionService.getAvailableUserActionItems(licence, null))
        .doesNotContain(LicenceActionItem.MANAGE_LICENSEES.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleExists() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    when(licenceScheduleDetailService.nonDeletedScheduleExistsForLicence(licence)).thenReturn(true);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, null))
        .doesNotContain(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleDoesNotExist() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    when(licenceScheduleDetailService.nonDeletedScheduleExistsForLicence(licence)).thenReturn(false);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, null))
        .contains(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }

  @Test
  void getAvailableUserActionItems_licenceScheduleDoesNotExist_licenceTypeNotSetupForSchedules() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.METHANE_DRAINAGE)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    when(licenceScheduleDetailService.nonDeletedScheduleExistsForLicence(licence)).thenReturn(false);

    assertThat(licenceActionService.getAvailableUserActionItems(licence, null))
        .doesNotContain(LicenceActionItem.CREATE_LICENCE_SCHEDULE.toActionItemView(licence));
  }
}