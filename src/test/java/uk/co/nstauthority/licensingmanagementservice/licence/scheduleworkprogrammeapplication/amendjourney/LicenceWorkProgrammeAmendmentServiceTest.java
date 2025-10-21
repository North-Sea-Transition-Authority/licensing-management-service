package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
class LicenceWorkProgrammeAmendmentServiceTest {

  @Mock
  private LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @Mock
  private LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  @InjectMocks
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

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

    when(licenceWorkProgrammeAmendmentRepository.findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(any(), any())).thenReturn(
        Optional.of(licenceWorkProgrammeAmendmentRequest));

    LicenceWorkProgrammeAmendmentForm actualForm = licenceWorkProgrammeAmendmentService.getLicenceWorkProgrammeActivityAmendmentForm(UUID.randomUUID(),
        scheduleWorkProgrammeApplicationDetail);

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

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,UUID.randomUUID());

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

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,UUID.randomUUID());

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

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,UUID.randomUUID());

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

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,UUID.randomUUID());

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

  @Test
  void validateAllWorkProgrammeAmendments_withValidAmendments() {
    when(licenceWorkProgrammeAmendmentFormValidator.isValid(any(), any())).thenReturn(true);

    LicenceWorkProgrammeAmendmentRequest request = new LicenceWorkProgrammeAmendmentRequest();
    request.setWorkProgrammeChangeRequested(true);
    request.setWorkProgrammeAmendmentInformation("testAmendmentInformation");
    request.setWorkProgrammeCompletionDateChangeRequested(true);
    request.setWorkProgrammeExtensionDuration(new ThreeFieldDuration(2, 2, 2));

    boolean result = licenceWorkProgrammeAmendmentService.validateAllWorkProgrammeAmendments(List.of(request));

    assertTrue(result);
    verify(licenceWorkProgrammeAmendmentFormValidator).isValid(any(LicenceWorkProgrammeAmendmentForm.class), any());
  }

  @Test
  void validateAllWorkProgrammeAmendments_withInvalidAmendments() {
    when(licenceWorkProgrammeAmendmentFormValidator.isValid(any(), any())).thenReturn(false);

    LicenceWorkProgrammeAmendmentRequest request = new LicenceWorkProgrammeAmendmentRequest();
    request.setWorkProgrammeChangeRequested(false);
    request.setWorkProgrammeCompletionDateChangeRequested(false);

    assertFalse(licenceWorkProgrammeAmendmentService.validateAllWorkProgrammeAmendments(List.of(request)));
    verify(licenceWorkProgrammeAmendmentFormValidator).isValid(any(LicenceWorkProgrammeAmendmentForm.class), any());
  }
}