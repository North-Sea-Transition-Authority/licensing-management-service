package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
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
class LicenceContinuationServiceTest {

  @Mock
  private LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;

  @Mock
  private LicenceContinuationApplicationRepository licenceContinuationApplicationRepository;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Mock
  private Clock clock;

  @InjectMocks
  private LicenceContinuationService licenceContinuationService;

  @Captor
  private ArgumentCaptor<LicenceContinuationApplication> licenceContinuationApplicationCaptor;

  @Captor
  private ArgumentCaptor<LicenceContinuationApplicationDetail> licenceContinuationApplicationDetailCaptor;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL
      = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));

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
}