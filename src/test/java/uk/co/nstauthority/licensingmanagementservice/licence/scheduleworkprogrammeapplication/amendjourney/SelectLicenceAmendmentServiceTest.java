package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class SelectLicenceAmendmentServiceTest {

  @Mock
  LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Captor
  private ArgumentCaptor<LicenceWorkProgrammeAmendmentRequest> licenceWorkProgrammeAmendmentRequestArgumentCaptor;

  @InjectMocks
  SelectLicenceAmendmentService selectLicenceAmendmentService;


  @Test
  void saveAmendmentForm_createsNewRequest_whenNoneExists() {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var licenceScheduleAmendmentForm = new SelectLicenceAmendmentForm();
    var workProgrammeActivityId = UUID.randomUUID();

    var workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setId(workProgrammeActivityId);

    licenceScheduleAmendmentForm.setSelectedWorkProgrammeActivityAmendmentId(workProgrammeActivityId);

    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivityId))
        .thenReturn(workProgrammeActivity);

    when(licenceWorkProgrammeAmendmentRepository.findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivity(
        any(ScheduleWorkProgrammeApplicationDetail.class),
        any(WorkProgrammeActivity.class)
    )).thenReturn(Optional.empty());

    selectLicenceAmendmentService.saveAmendmentForm(workProgrammeActivityId, licenceScheduleAmendmentForm,
                                                    scheduleWorkProgrammeApplicationDetail);


    verify(licenceWorkProgrammeAmendmentRepository).save(licenceWorkProgrammeAmendmentRequestArgumentCaptor.capture());

    var result = licenceWorkProgrammeAmendmentRequestArgumentCaptor.getValue();

    assertThat(result.getId()).isNull();

    assertThat(result).extracting(
        LicenceWorkProgrammeAmendmentRequest::getScheduleWorkProgrammeApplicationDetails,
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeActivity
    ).containsExactly(
        scheduleWorkProgrammeApplicationDetail,
        workProgrammeActivity
    );

    verify(workProgrammeActivityService, times(2))
        .getWorkProgrammeActivityByIdOrThrow(workProgrammeActivityId);  }
}