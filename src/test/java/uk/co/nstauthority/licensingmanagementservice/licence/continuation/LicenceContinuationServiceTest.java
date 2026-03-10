package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
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
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationServiceTest {

  @Mock
  private LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;

  @Mock
  private LicenceContinuationApplicationRepository licenceContinuationApplicationRepository;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Mock
  private Clock clock;

  @Mock
  private ApplicationAccessService applicationAccessService;

  @InjectMocks
  private LicenceContinuationService licenceContinuationService;

  @Captor
  private ArgumentCaptor<LicenceContinuationApplication> licenceContinuationApplicationCaptor;

  @Captor
  private ArgumentCaptor<LicenceContinuationApplicationDetail> licenceContinuationApplicationDetailCaptor;

  private static final Instant FIXED_INSTANT = Instant.now();
  private static final long WUA_ID = 1L;
  private static final int ORG_UNIT_ID = 1;
  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL
      = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private ServiceUserDetail organisationUser;

  @BeforeEach
  void setUp() {
    var licenceContinuationApplication = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplication(LICENCE_SCHEDULE_DETAIL);
    licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceContinuationApplication(licenceContinuationApplication)
        .withStatus(LicenceContinuationApplicationStatus.DRAFT)
        .build();

    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(WUA_ID)
        .build();
  }

  @Test
  void createNewLicenceContinuationApplication_withValidLicence_createsAndSavesEntities() {
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(LICENCE, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(LICENCE_SCHEDULE_DETAIL);

    var result = licenceContinuationService.createNewLicenceContinuationApplication(LICENCE, 1);

    verify(licenceContinuationApplicationRepository).save(licenceContinuationApplicationCaptor.capture());
    var savedApplication = licenceContinuationApplicationCaptor.getValue();

    verify(licenceContinuationApplicationDetailRepository).save(licenceContinuationApplicationDetailCaptor.capture());
    var savedDetail = licenceContinuationApplicationDetailCaptor.getValue();

    assertThat(savedApplication.getLicenceScheduleDetail()).isEqualTo(LICENCE_SCHEDULE_DETAIL);

    assertThat(savedDetail.getLicenceContinuationApplication()).isEqualTo(savedApplication);
    assertThat(savedDetail.getVersionNumber()).isEqualTo(1);
    assertThat(savedDetail.getStatus()).isEqualTo(LicenceContinuationApplicationStatus.DRAFT);
    assertThat(savedDetail.getResponsibleOrganisationUnitId()).isEqualTo(1);

    assertThat(result).isEqualTo(savedDetail);
  }

  @Test
  void getLicenceFromScheduleWorkProgrammeApplicationDetail_withValidDetail_returnsLicence() {
    var licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(
        LICENCE_SCHEDULE_DETAIL);
    Licence result = licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail);
    assertThat(result).isEqualTo(LICENCE);
  }

  @Test
  void getAllContinuationApplicationDetailsByStatus() {
    licenceContinuationService.getAllContinuationApplicationDetailsByStatus(
        LicenceContinuationApplicationStatus.DRAFT);

    verify(licenceContinuationApplicationDetailRepository).findAllByStatus(LicenceContinuationApplicationStatus.DRAFT);
  }

  @Test
  void getAllContinuationApplicationDetailsByStatuses() {
    var statusSet = Set.of(
        LicenceContinuationApplicationStatus.DRAFT,
        LicenceContinuationApplicationStatus.SUBMITTED
    );

    licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(statusSet);

    verify(licenceContinuationApplicationDetailRepository).findAllByStatusIn(statusSet);
  }

  @Test
  void submitApplication_withValidDetail_setsReferenceStatusSubmitted() {
    when(clock.instant()).thenReturn(FIXED_INSTANT);
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    int existingSubmissions = 5;
    when(licenceContinuationApplicationDetailRepository.countByVersionNumberAndStatusAndSubmittedDatetimeBetween(
        eq(1),
        eq(LicenceContinuationApplicationStatus.SUBMITTED),
        any(),
        any()
    )).thenReturn(existingSubmissions);


    LicenceContinuationApplication result = licenceContinuationService.submitApplication(licenceContinuationApplicationDetail, organisationUser);

    verify(licenceContinuationApplicationRepository).save(licenceContinuationApplicationCaptor.capture());
    verify(licenceContinuationApplicationDetailRepository).save(licenceContinuationApplicationDetailCaptor.capture());

    LicenceContinuationApplication savedApp = licenceContinuationApplicationCaptor.getValue();
    LicenceContinuationApplicationDetail savedDetail = licenceContinuationApplicationDetailCaptor.getValue();

    assertThat(savedApp).isEqualTo(result);
    assertThat(savedApp.getApplicationReference()).isNotNull();


    assertThat(savedDetail.getStatus()).isEqualTo(LicenceContinuationApplicationStatus.SUBMITTED);
    assertThat(savedDetail.getSubmittedDatetime()).isEqualTo(FIXED_INSTANT);
    assertThat(savedDetail.getSubmittedByWuaId()).isEqualTo(WUA_ID);
  }

  @Test
  void userCanSubmitApplication_returnsTrue_whenUserHasSubmitterRoleForOrg() {
    licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    licenceContinuationApplicationDetail.setResponsibleOrganisationUnitId(ORG_UNIT_ID);

    when(applicationAccessService.userIsSubmitterForOrganisationUnit(ORG_UNIT_ID, WUA_ID)).thenReturn(true);

    boolean result = licenceContinuationService.userCanSubmitApplication(licenceContinuationApplicationDetail, organisationUser);

    assertThat(result).isTrue();
    verify(applicationAccessService).userIsSubmitterForOrganisationUnit(ORG_UNIT_ID, WUA_ID);
  }

  @Test
  void userCanSubmitApplication_returnsFalse_whenUserDoesNotHaveSubmitterRoleForOrg() {
    licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    licenceContinuationApplicationDetail.setResponsibleOrganisationUnitId(ORG_UNIT_ID);

    when(applicationAccessService.userIsSubmitterForOrganisationUnit(ORG_UNIT_ID, WUA_ID)).thenReturn(false);

    boolean result = licenceContinuationService.userCanSubmitApplication(licenceContinuationApplicationDetail, organisationUser);

    assertThat(result).isFalse();
    verify(applicationAccessService).userIsSubmitterForOrganisationUnit(ORG_UNIT_ID, WUA_ID);
  }
}