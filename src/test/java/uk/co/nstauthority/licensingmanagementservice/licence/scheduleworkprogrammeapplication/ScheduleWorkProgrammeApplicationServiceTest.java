package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository;

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

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
}