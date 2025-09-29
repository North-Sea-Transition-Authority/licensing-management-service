package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceWorkProgrammeAmendmentFormServiceTest {

  @Mock
  private LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @Mock
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  @InjectMocks
  private LicenceWorkProgrammeAmendmentFormService licenceWorkProgrammeAmendmentFormService;

  @Captor
  private ArgumentCaptor<LicenceWorkProgrammeAmendmentRequest> licenceWorkProgrammeAmendmentRequestArgumentCaptor;

  @Test
  void getLicenceScheduleExtensionExisting() {
    ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeExtensionDuration(new ThreeFieldDuration(1, 1, 1));
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeAmendmentInformation("testInformation");
    licenceWorkProgrammeAmendmentRequest.setId(UUID.randomUUID());
    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    when(licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
        any(),any())).thenReturn(Optional.of(licenceWorkProgrammeAmendmentRequest));

    LicenceWorkProgrammeAmendmentForm actualForm = licenceWorkProgrammeAmendmentFormService.getLicenceWorkProgramAmendmentForm(
        scheduleWorkProgrammeApplicationDetail,UUID.randomUUID());

    assertEquals(actualForm.getWorkProgrammeExtensionDuration().toThreeFieldDuration(),
        licenceWorkProgrammeAmendmentRequest.getWorkProgrammeExtensionDuration());
    assertEquals(("testInformation"), actualForm.getWorkProgrammeAmendmentInformation());
  }

  @Test
  void saveAmendmentFormYesThenNoScenario() {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setWorkProgrammeAmendmentInformation("testAmendmentInformation");
    form.getWorkProgrammeExtensionDuration().setYears("4");
    form.getWorkProgrammeExtensionDuration().setMonths("4");
    form.getWorkProgrammeExtensionDuration().setDays("4");
    form.setDurationExtensionRequired(true);
    form.setAdditionalInfoRequired(true);

    licenceWorkProgrammeAmendmentFormService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,UUID.randomUUID());

    verify(licenceWorkProgrammeAmendmentRepository).save(licenceWorkProgrammeAmendmentRequestArgumentCaptor.capture());

    var result = licenceWorkProgrammeAmendmentRequestArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeExtensionDuration,
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeAmendmentInformation,
        LicenceWorkProgrammeAmendmentRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsExactly(
        form.getWorkProgrammeExtensionDuration().toThreeFieldDuration(),
        form.getWorkProgrammeAmendmentInformation(),
        scheduleWorkProgrammeApplicationDetail
    );

    form.setDurationExtensionRequired(false);
    form.setAdditionalInfoRequired(false);

    licenceWorkProgrammeAmendmentFormService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,UUID.randomUUID());

    verify(licenceWorkProgrammeAmendmentRepository,times(2)).save(licenceWorkProgrammeAmendmentRequestArgumentCaptor.capture());

    var updatedResult = licenceWorkProgrammeAmendmentRequestArgumentCaptor.getValue();

    assertThat(updatedResult).extracting(
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeExtensionDuration,
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeAmendmentInformation,
        LicenceWorkProgrammeAmendmentRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsNull();

  }

  @Test
  void saveAmendmentForm() {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setWorkProgrammeAmendmentInformation("testAmendmentInformation");
    form.getWorkProgrammeExtensionDuration().setYears("4");
    form.getWorkProgrammeExtensionDuration().setMonths("4");
    form.getWorkProgrammeExtensionDuration().setDays("4");
    form.setDurationExtensionRequired(true);
    form.setAdditionalInfoRequired(true);

    licenceWorkProgrammeAmendmentFormService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,UUID.randomUUID());

    verify(licenceWorkProgrammeAmendmentRepository).save(licenceWorkProgrammeAmendmentRequestArgumentCaptor.capture());

    var result = licenceWorkProgrammeAmendmentRequestArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeExtensionDuration,
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeAmendmentInformation,
        LicenceWorkProgrammeAmendmentRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsExactly(
        form.getWorkProgrammeExtensionDuration().toThreeFieldDuration(),
        form.getWorkProgrammeAmendmentInformation(),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @Test
  void saveAmendmentFormWithoutExtension() {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setWorkProgrammeAmendmentInformation("testAmendmentInformation");
    form.setAdditionalInfoRequired(true);

    licenceWorkProgrammeAmendmentFormService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,UUID.randomUUID());

    verify(licenceWorkProgrammeAmendmentRepository).save(licenceWorkProgrammeAmendmentRequestArgumentCaptor.capture());

    var result = licenceWorkProgrammeAmendmentRequestArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceWorkProgrammeAmendmentRequest::getWorkProgrammeAmendmentInformation,
        LicenceWorkProgrammeAmendmentRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsExactly(
        form.getWorkProgrammeAmendmentInformation(),
        scheduleWorkProgrammeApplicationDetail
    );
  }


}