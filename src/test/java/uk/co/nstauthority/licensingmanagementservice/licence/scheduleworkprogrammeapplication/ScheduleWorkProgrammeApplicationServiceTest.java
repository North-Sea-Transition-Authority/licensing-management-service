package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.LicenseeInformationForm;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationServiceTest {

  private static final Instant CURRENT_INSTANT = Instant.now();

  @Mock
  private ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository;

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Mock
  private Clock clock;

  @Mock
  private ApplicationAccessService applicationAccessService;

  @Mock
  private TeamManagementService teamManagementService;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Captor
  private ArgumentCaptor<ScheduleWorkProgrammeApplication> scheduleWorkProgrammeApplicationCaptor;

  @Captor
  private ArgumentCaptor<ScheduleWorkProgrammeApplicationDetail> scheduleWorkProgrammeApplicationDetailCaptor;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;


  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().build();
    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(licence));
    scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withScheduleWorkProgrammeApplication(
            ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail))
        .build();
  }

  @Test
  void getLicenceFromScheduleWorkProgrammeApplicationDetail_withValidDetail_returnsLicence() {
    Licence result = scheduleWorkProgrammeApplicationDetail.getLicence();
    assertThat(result).isEqualTo(licence);
  }

  @Test
  void getScheduleDetailFromApplicationDetail_whenDraft_returnsActiveDetail() {
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(licenceScheduleDetail);

    var result = scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    assertThat(result).isEqualTo(licenceScheduleDetail);
  }

  @Test
  void getScheduleDetailFromApplicationDetail_whenSubmitted_returnsFrozenDetail() {
    var frozenDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceScheduleDetail.getLicenceSchedule());
    scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication()
        .setSubmittedLicenceScheduleDetail(frozenDetail);

    var result = scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    assertThat(result).isEqualTo(frozenDetail);
  }

  @Test
  void getAllScheduleWorkProgrammeApplicationDetailsByStatuses() {
    scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(Set.of(ApplicationStatus.DRAFT));

    verify(scheduleWorkProgrammeApplicationDetailRepository).findAllByStatusIn(Set.of(ApplicationStatus.DRAFT));
  }

  @Test
  void createNewScheduleWorkProgrammeApplicationForLicence_withValidLicenceAndPermissionTrue() {
    var savedApplication = new ScheduleWorkProgrammeApplication();
    savedApplication.setId(UUID.randomUUID());

    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(licenceScheduleDetail);
    when(clock.instant()).thenReturn(CURRENT_INSTANT);
    when(scheduleWorkProgrammeApplicationRepository.save(any(ScheduleWorkProgrammeApplication.class)))
        .thenReturn(savedApplication);

    LicenseeInformationForm licenseeInformationForm = new LicenseeInformationForm();
    licenseeInformationForm.setAllLicenseesPermissionConfirmed(true);
    licenseeInformationForm.setResponsibleOrganisationUnitId(1);

    ScheduleWorkProgrammeApplicationDetail result = scheduleWorkProgrammeApplicationService.createNewScheduleWorkProgrammeApplicationForLicence(licence,
        licenseeInformationForm);

    verify(scheduleWorkProgrammeApplicationRepository).save(scheduleWorkProgrammeApplicationCaptor.capture());
    assertThat(scheduleWorkProgrammeApplicationCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceScheduleDetail.getLicenceSchedule());

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    ScheduleWorkProgrammeApplicationDetail savedDetail = scheduleWorkProgrammeApplicationDetailCaptor.getValue();
    assertThat(savedDetail.getScheduleWorkProgrammeApplication()).isEqualTo(savedApplication);
    assertThat(savedDetail.getVersionNumber()).isEqualTo(1);
    assertThat(savedDetail.getAllLicenseesPermissionConfirmed()).isTrue();
    assertThat(savedDetail.getResponsibleOrganisationUnitId()).isEqualTo(1);
    assertThat(savedDetail.getCreatedDatetime()).isEqualTo(CURRENT_INSTANT);
    assertThat(result).isEqualTo(savedDetail);

    verify(teamManagementService).createScopedTeam(
        eq(TeamType.EXTERNAL_CONTRIBUTORS.getDisplayName()),
        eq(TeamType.EXTERNAL_CONTRIBUTORS),
        any()
    );
  }

  @Test
  void createNewScheduleWorkProgrammeApplicationForLicence_withValidLicenceAndPermissionFalse() {
    var savedApplication = new ScheduleWorkProgrammeApplication();
    savedApplication.setId(UUID.randomUUID());

    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(licenceScheduleDetail);
    when(clock.instant()).thenReturn(CURRENT_INSTANT);
    when(scheduleWorkProgrammeApplicationRepository.save(any(ScheduleWorkProgrammeApplication.class)))
        .thenReturn(savedApplication);

    LicenseeInformationForm licenseeInformationForm = new LicenseeInformationForm();
    licenseeInformationForm.setAllLicenseesPermissionConfirmed(false);
    licenseeInformationForm.setResponsibleOrganisationUnitId(1);
    ScheduleWorkProgrammeApplicationDetail result = scheduleWorkProgrammeApplicationService.createNewScheduleWorkProgrammeApplicationForLicence(licence, licenseeInformationForm);

    verify(scheduleWorkProgrammeApplicationRepository).save(scheduleWorkProgrammeApplicationCaptor.capture());
    assertThat(scheduleWorkProgrammeApplicationCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceScheduleDetail.getLicenceSchedule());

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    var savedDetail = scheduleWorkProgrammeApplicationDetailCaptor.getValue();
    assertThat(savedDetail.getScheduleWorkProgrammeApplication()).isEqualTo(savedApplication);
    assertThat(savedDetail.getVersionNumber()).isEqualTo(1);
    assertThat(savedDetail.getAllLicenseesPermissionConfirmed()).isFalse();
    assertThat(savedDetail.getResponsibleOrganisationUnitId()).isEqualTo(1);
    assertThat(savedDetail.getCreatedDatetime()).isEqualTo(CURRENT_INSTANT);

    assertThat(result).isEqualTo(savedDetail);

    verify(teamManagementService).createScopedTeam(
        eq(TeamType.EXTERNAL_CONTRIBUTORS.getDisplayName()),
        eq(TeamType.EXTERNAL_CONTRIBUTORS),
        any(TeamScopeReference.class)
    );
  }

  @Test
  void submitApplication_withValidDetail_setsReferenceStatusSubmitted() {
    when(clock.instant()).thenReturn(CURRENT_INSTANT);
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    when(scheduleWorkProgrammeApplicationDetailRepository.countByVersionNumberAndSubmittedDatetimeBetween(
        eq(1),
        any(Instant.class),
        any(Instant.class)
    )).thenReturn(2);

    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(licenceScheduleDetail);

    var currentYear = LocalDate.now(clock).getYear();

    var user = mockUser();

    var result = scheduleWorkProgrammeApplicationService.submitApplication(scheduleWorkProgrammeApplicationDetail, user);

    verify(scheduleWorkProgrammeApplicationRepository).save(scheduleWorkProgrammeApplicationCaptor.capture());
    ScheduleWorkProgrammeApplication savedApplication = scheduleWorkProgrammeApplicationCaptor.getValue();

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    ScheduleWorkProgrammeApplicationDetail savedDetail = scheduleWorkProgrammeApplicationDetailCaptor.getValue();

    assertThat(savedApplication.getApplicationReference()).isEqualTo(String.format("LMS/EAA/%d/%d", currentYear, 3));
    assertThat(savedApplication.getSubmittedLicenceScheduleDetail()).isEqualTo(licenceScheduleDetail);
    assertThat(result).isEqualTo(savedApplication);
    assertThat(savedDetail.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    assertThat(savedDetail.getSubmittedByWuaId()).isEqualTo(1L);
    assertThat(savedDetail.getSubmittedDatetime()).isEqualTo(Instant.now(clock));
  }

  @Test
  void deleteScheduleWorkProgrammeApplication_setsStatusToDeletedAndSaves() {
    scheduleWorkProgrammeApplicationDetail.setStatus(ApplicationStatus.DRAFT);
    scheduleWorkProgrammeApplicationService.deleteScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplicationDetail);

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    ScheduleWorkProgrammeApplicationDetail savedEntity = scheduleWorkProgrammeApplicationDetailCaptor.getValue();
    assertThat(savedEntity.getStatus()).isEqualTo(ApplicationStatus.DELETED);
  }

  @Test
  void userCanSubmitApplication() {
    var user = mockUser();
    var organisationUnitId = 100;
    scheduleWorkProgrammeApplicationDetail.setResponsibleOrganisationUnitId(organisationUnitId);

    scheduleWorkProgrammeApplicationService.userCanSubmitApplication(scheduleWorkProgrammeApplicationDetail, user);

    verify(applicationAccessService).userIsSubmitterForOrganisationUnit(organisationUnitId, user.wuaId());
  }

  private ServiceUserDetail mockUser() {
    var user = mock(ServiceUserDetail.class);
    when(user.wuaId()).thenReturn(1L);
    return user;
  }
}