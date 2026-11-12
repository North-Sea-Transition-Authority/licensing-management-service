package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateSubmittedPayload;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationUpdateSubmittedEmailService;
import uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker.CrossLicenceEventTrackerService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.LicenseeInformationForm;
import uk.co.nstauthority.licensingmanagementservice.teams.IndustryRoleService;
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
  private IndustryRoleService industryRoleService;

  @Mock
  private TeamManagementService teamManagementService;

  @Mock
  private ScheduleWorkProgrammeApplicationSubmittedNotificationService applicationSubmittedNotificationService;

  @Mock
  private ApplicationUpdateSubmittedEmailService applicationUpdateSubmittedEmailService;

  @Mock
  private CrossLicenceEventTrackerService crossLicenceEventTrackerService;

  @Mock
  private CaseEventService caseEventService;

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRequestCache scheduleWorkProgrammeApplicationDetailRequestCache;

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
  void getDetailByIdOrThrow_whenDetailFound_thenReturnsIt() {
    var detailId = scheduleWorkProgrammeApplicationDetail.getId();
    when(scheduleWorkProgrammeApplicationDetailRequestCache.findById(detailId))
        .thenReturn(Optional.of(scheduleWorkProgrammeApplicationDetail));

    var result = scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(detailId);

    assertThat(result).isEqualTo(scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void getDetailByIdOrThrow_whenDetailNotFound_thenThrows() {
    var detailId = UUID.randomUUID();
    when(scheduleWorkProgrammeApplicationDetailRequestCache.findById(detailId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(detailId))
        .isInstanceOf(LmsEntityNotFoundException.class);
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
  void getCurrentScheduleDetailFromApplicationDetail_whenSubmitted_returnsActiveDetailNotFrozenDetail() {
    var frozenDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceScheduleDetail.getLicenceSchedule());
    scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication()
        .setSubmittedLicenceScheduleDetail(frozenDetail);
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(licenceScheduleDetail);

    var result = scheduleWorkProgrammeApplicationService
        .getCurrentScheduleDetailFromApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    assertThat(result).isEqualTo(licenceScheduleDetail);
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
        licenseeInformationForm, mockUser());

    verify(scheduleWorkProgrammeApplicationRepository).save(scheduleWorkProgrammeApplicationCaptor.capture());
    assertThat(scheduleWorkProgrammeApplicationCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceScheduleDetail.getLicenceSchedule());

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    ScheduleWorkProgrammeApplicationDetail savedDetail = scheduleWorkProgrammeApplicationDetailCaptor.getValue();
    assertThat(savedDetail.getScheduleWorkProgrammeApplication()).isEqualTo(savedApplication);
    assertThat(savedDetail.getVersionNumber()).isEqualTo(1);
    assertThat(savedDetail.getAllLicenseesPermissionConfirmed()).isTrue();
    assertThat(savedDetail.getResponsibleOrganisationUnitId()).isEqualTo(1);
    assertThat(savedDetail.getCreatedDatetime()).isEqualTo(CURRENT_INSTANT);
    assertThat(savedDetail.getCreatedByWuaId()).isEqualTo(1L);
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
    ScheduleWorkProgrammeApplicationDetail result = scheduleWorkProgrammeApplicationService.createNewScheduleWorkProgrammeApplicationForLicence(licence, licenseeInformationForm, mockUser());

    verify(scheduleWorkProgrammeApplicationRepository).save(scheduleWorkProgrammeApplicationCaptor.capture());
    assertThat(scheduleWorkProgrammeApplicationCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceScheduleDetail.getLicenceSchedule());

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    var savedDetail = scheduleWorkProgrammeApplicationDetailCaptor.getValue();
    assertThat(savedDetail.getScheduleWorkProgrammeApplication()).isEqualTo(savedApplication);
    assertThat(savedDetail.getVersionNumber()).isEqualTo(1);
    assertThat(savedDetail.getAllLicenseesPermissionConfirmed()).isFalse();
    assertThat(savedDetail.getResponsibleOrganisationUnitId()).isEqualTo(1);
    assertThat(savedDetail.getCreatedDatetime()).isEqualTo(CURRENT_INSTANT);
    assertThat(savedDetail.getCreatedByWuaId()).isEqualTo(1L);

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

    assertThat(savedApplication.getApplicationReference()).isEqualTo(String.format("LMS/EAA/%d/%d/1", currentYear, 3));
    assertThat(savedApplication.getSubmittedLicenceScheduleDetail()).isEqualTo(licenceScheduleDetail);
    assertThat(result).isEqualTo(savedApplication);
    assertThat(savedDetail.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    assertThat(savedDetail.getSubmittedByWuaId()).isEqualTo(1L);
    assertThat(savedDetail.getSubmittedDatetime()).isEqualTo(Instant.now(clock));

    verify(applicationSubmittedNotificationService).sendNewApplicationSubmittedEmails(scheduleWorkProgrammeApplicationDetail);
    verify(crossLicenceEventTrackerService).refreshApplicationCache(savedApplication);
    verify(caseEventService).recordCaseEvent(CaseEventType.APPLICATION_SUBMITTED, savedApplication, null);
  }

  @Test
  void submitApplication_whenApplicationAlreadyHasReference_keepsReferenceAndUpdatesVersionSuffix() {
    when(clock.instant()).thenReturn(CURRENT_INSTANT);
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(licenceScheduleDetail);
    scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication().setApplicationReference("LMS/EAA/2024/5/2");
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(3);

    var result = scheduleWorkProgrammeApplicationService.submitApplication(scheduleWorkProgrammeApplicationDetail, mockUser());

    assertThat(result.getApplicationReference()).isEqualTo("LMS/EAA/2024/5/3");
    verify(scheduleWorkProgrammeApplicationDetailRepository, never())
        .countByVersionNumberAndSubmittedDatetimeBetween(anyInt(), any(Instant.class), any(Instant.class));
  }

  @Test
  void submitApplication_whenUpdateDraft_sendsUpdateSubmittedEmailsInsteadOfNewApplicationEmails() {
    when(clock.instant()).thenReturn(CURRENT_INSTANT);
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(licenceScheduleDetail);
    scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication().setApplicationReference("LMS/EAA/2024/5/1");
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(2);
    scheduleWorkProgrammeApplicationDetail.setUpdateDeadlineDate(LocalDate.of(2024, Month.APRIL, 1));
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(2);

    scheduleWorkProgrammeApplicationService.submitApplication(scheduleWorkProgrammeApplicationDetail, mockUser());

    verify(applicationUpdateSubmittedEmailService).sendUpdateSubmittedEmails(scheduleWorkProgrammeApplicationDetail);
    verifyNoInteractions(applicationSubmittedNotificationService);
    verify(caseEventService).recordCaseEvent(
        CaseEventType.APPLICATION_UPDATE_SUBMITTED,
        scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication(),
        new CaseUpdateSubmittedPayload(2)
    );
    verify(caseEventService, never()).recordCaseEvent(eq(CaseEventType.APPLICATION_SUBMITTED), any(), any());
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
  void issueDecision_setsStatusToDecisionRecordedAndSaves() {
    scheduleWorkProgrammeApplicationDetail.setStatus(ApplicationStatus.DSP_UPLOADED);
    scheduleWorkProgrammeApplicationService.decisionRecorded(scheduleWorkProgrammeApplicationDetail);

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    ScheduleWorkProgrammeApplicationDetail savedEntity = scheduleWorkProgrammeApplicationDetailCaptor.getValue();
    assertThat(savedEntity.getStatus()).isEqualTo(ApplicationStatus.ISSUE_DECISION);
  }

  @Test
  void draftDecisionCreated_whenLatestDetailExists_thenSetsStatusToDraftDecision() {
    var application = scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplicationDetail.setStatus(ApplicationStatus.DSP_UPLOADED);
    when(scheduleWorkProgrammeApplicationDetailRepository
        .findFirstByScheduleWorkProgrammeApplicationAndStatusNotOrderByVersionNumberDesc(
            application,
            ApplicationStatus.DELETED
        ))
        .thenReturn(Optional.of(scheduleWorkProgrammeApplicationDetail));

    scheduleWorkProgrammeApplicationService.draftDecisionCreated(application);

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    assertThat(scheduleWorkProgrammeApplicationDetailCaptor.getValue().getStatus())
        .isEqualTo(ApplicationStatus.DRAFT_DECISION);
  }

  @Test
  void draftDecisionCreated_whenNoLatestDetail_thenThrows() {
    var application = scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication();
    when(scheduleWorkProgrammeApplicationDetailRepository
        .findFirstByScheduleWorkProgrammeApplicationAndStatusNotOrderByVersionNumberDesc(
            application,
            ApplicationStatus.DELETED
        ))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> scheduleWorkProgrammeApplicationService.draftDecisionCreated(application))
        .isInstanceOf(LmsEntityNotFoundException.class);
    verify(scheduleWorkProgrammeApplicationDetailRepository, never())
        .save(any(ScheduleWorkProgrammeApplicationDetail.class));
  }

  @Test
  void userCanSubmitApplication() {
    var user = mockUser();
    var organisationUnitId = 100;
    scheduleWorkProgrammeApplicationDetail.setResponsibleOrganisationUnitId(organisationUnitId);

    scheduleWorkProgrammeApplicationService.userCanSubmitApplication(scheduleWorkProgrammeApplicationDetail, user);

    verify(industryRoleService).userIsSubmitterForOrganisationUnit(organisationUnitId, user.wuaId());
  }

  private ServiceUserDetail mockUser() {
    var user = mock(ServiceUserDetail.class);
    when(user.wuaId()).thenReturn(1L);
    return user;
  }
}
