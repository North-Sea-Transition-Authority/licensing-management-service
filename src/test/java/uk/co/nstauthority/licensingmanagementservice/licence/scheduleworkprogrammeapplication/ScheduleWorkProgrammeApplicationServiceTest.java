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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

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
    scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withScheduleWorkProgrammeApplication(
            ScheduleWorkProgrammeApplicationTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail))
        .build();
  }

  @Test
  void getLicenceFromScheduleWorkProgrammeApplicationDetail_withValidDetail_returnsLicence() {
    Licence result = scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);
    assertThat(result).isEqualTo(licence);
  }

  @Test
  void getAllScheduleWorkProgrammeApplicationDetailsByStatus() {
    scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT);

    verify(scheduleWorkProgrammeApplicationDetailRepository).findAllByStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT);
  }

  @Test
  void createNewScheduleWorkProgrammeApplicationForLicence_withValidLicenceAndPermissionTrue() {
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(licenceScheduleDetail);

    ScheduleWorkProgrammeApplicationDetail result = scheduleWorkProgrammeApplicationService.createNewScheduleWorkProgrammeApplicationForLicence(licence, true);

    verify(scheduleWorkProgrammeApplicationRepository).save(scheduleWorkProgrammeApplicationCaptor.capture());
    ScheduleWorkProgrammeApplication savedScheduleWorkProgrammeApplication = scheduleWorkProgrammeApplicationCaptor.getValue();
    assertThat(savedScheduleWorkProgrammeApplication.getLicenceScheduleDetail()).isEqualTo(licenceScheduleDetail);

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    ScheduleWorkProgrammeApplicationDetail savedDetail = scheduleWorkProgrammeApplicationDetailCaptor.getValue();
    assertThat(savedDetail.getScheduleWorkProgrammeApplication()).isEqualTo(savedScheduleWorkProgrammeApplication);
    assertThat(savedDetail.getVersionNumber()).isEqualTo(1);
    assertThat(savedDetail.getAllLicenseesPermissionConfirmed()).isTrue();

    assertThat(result).isEqualTo(savedDetail);
  }

  @Test
  void createNewScheduleWorkProgrammeApplicationForLicence_withValidLicenceAndPermissionFalse() {
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(licenceScheduleDetail);

    ScheduleWorkProgrammeApplicationDetail result = scheduleWorkProgrammeApplicationService.createNewScheduleWorkProgrammeApplicationForLicence(licence, false);

    verify(scheduleWorkProgrammeApplicationRepository).save(scheduleWorkProgrammeApplicationCaptor.capture());
    var savedScheduleWorkProgrammeApplication = scheduleWorkProgrammeApplicationCaptor.getValue();
    assertThat(savedScheduleWorkProgrammeApplication.getLicenceScheduleDetail()).isEqualTo(licenceScheduleDetail);

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    var savedDetail = scheduleWorkProgrammeApplicationDetailCaptor.getValue();
    assertThat(savedDetail.getScheduleWorkProgrammeApplication()).isEqualTo(savedScheduleWorkProgrammeApplication);
    assertThat(savedDetail.getVersionNumber()).isEqualTo(1);
    assertThat(savedDetail.getAllLicenseesPermissionConfirmed()).isFalse();

    assertThat(result).isEqualTo(savedDetail);
  }

  @Test
  void submitApplication_withValidDetail_setsReferenceStatusSubmitted() {
    when(clock.instant()).thenReturn(CURRENT_INSTANT);
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    when(scheduleWorkProgrammeApplicationDetailRepository.countByVersionNumberAndStatusAndSubmittedDatetimeBetween(
        eq(1),
        eq(ScheduleWorkProgrammeApplicationStatus.SUBMITTED),
        any(Instant.class),
        any(Instant.class)
    )).thenReturn(2);

    var currentYear = LocalDate.now(clock).getYear();

    var user = mock(ServiceUserDetail.class);
    when(user.wuaId()).thenReturn(1L);

    var result = scheduleWorkProgrammeApplicationService.submitApplication(scheduleWorkProgrammeApplicationDetail, user);

    verify(scheduleWorkProgrammeApplicationRepository).save(scheduleWorkProgrammeApplicationCaptor.capture());
    ScheduleWorkProgrammeApplication savedApplication = scheduleWorkProgrammeApplicationCaptor.getValue();

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(scheduleWorkProgrammeApplicationDetailCaptor.capture());
    ScheduleWorkProgrammeApplicationDetail savedDetail = scheduleWorkProgrammeApplicationDetailCaptor.getValue();

    assertThat(savedApplication.getApplicationReference()).isEqualTo(String.format("LMS/EAA/%d/%d", currentYear, 3));
    assertThat(result).isEqualTo(savedApplication);
    assertThat(savedDetail.getStatus()).isEqualTo(ScheduleWorkProgrammeApplicationStatus.SUBMITTED);
    assertThat(savedDetail.getSubmittedByWuaId()).isEqualTo(1L);
    assertThat(savedDetail.getSubmittedDatetime()).isEqualTo(Instant.now(clock));
  }
}