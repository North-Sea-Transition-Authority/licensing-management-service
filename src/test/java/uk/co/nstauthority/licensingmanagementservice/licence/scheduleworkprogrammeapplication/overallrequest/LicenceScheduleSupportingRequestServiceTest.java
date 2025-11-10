package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleSupportingRequestServiceTest {

  @Mock
  private LicenceScheduleSupportingRequestRepository licenceScheduleSupportingRequestRepository;

  @InjectMocks
  private LicenceScheduleSupportingRequestService licenceScheduleSupportingRequestService;

  @Captor
  private ArgumentCaptor<LicenceScheduleSupportingRequest> licenceScheduleSupportingRequestArgumentCaptor;

  @Test
  void saveRequestForm_existingOverallRequest() {
    ScheduleWorkProgrammeApplicationDetail detail = new ScheduleWorkProgrammeApplicationDetail();
    LicenceScheduleSupportingRequest existingRequest = new LicenceScheduleSupportingRequest();
    existingRequest.setLicenceProgress("Old Info");

    LicenceScheduleSupportingRequestForm form = new LicenceScheduleSupportingRequestForm();
    form.setLicenceProgress("New Progress Info");
    form.setReasonForAmendment("New Reason");
    form.setImpactOnDeliverables("New Impact");
    form.setPlanDuringExtension("New Plan");

    when(licenceScheduleSupportingRequestRepository.findByScheduleWorkProgrammeApplicationDetails(detail))
        .thenReturn(Optional.of(existingRequest));

    licenceScheduleSupportingRequestService.saveRequestForm(form, detail);

    verify(licenceScheduleSupportingRequestRepository).save(licenceScheduleSupportingRequestArgumentCaptor.capture());

    LicenceScheduleSupportingRequest savedRequest = licenceScheduleSupportingRequestArgumentCaptor.getValue();
    assertEquals("New Progress Info", savedRequest.getLicenceProgress());
    assertEquals("New Reason", savedRequest.getReasonForAmendment());
    assertEquals("New Impact", savedRequest.getImpactOnDeliverables());
    assertEquals("New Plan", savedRequest.getPlanDuringExtension());
    assertEquals(detail, savedRequest.getScheduleWorkProgrammeApplicationDetails());
    assertEquals(existingRequest, savedRequest);
  }

  @Test
  void saveRequestForm_newOverallRequest() {
    ScheduleWorkProgrammeApplicationDetail detail = new ScheduleWorkProgrammeApplicationDetail();

    LicenceScheduleSupportingRequestForm form = new LicenceScheduleSupportingRequestForm();
    form.setLicenceProgress("New Progress Info");
    form.setReasonForAmendment("New Reason");
    form.setImpactOnDeliverables("New Impact");
    form.setPlanDuringExtension("New Plan");

    when(licenceScheduleSupportingRequestRepository.findByScheduleWorkProgrammeApplicationDetails(detail))
        .thenReturn(Optional.empty());

    licenceScheduleSupportingRequestService.saveRequestForm(form, detail);

    verify(licenceScheduleSupportingRequestRepository).save(licenceScheduleSupportingRequestArgumentCaptor.capture());

    LicenceScheduleSupportingRequest savedRequest = licenceScheduleSupportingRequestArgumentCaptor.getValue();
    assertEquals("New Progress Info", savedRequest.getLicenceProgress());
    assertEquals("New Reason", savedRequest.getReasonForAmendment());
    assertEquals("New Impact", savedRequest.getImpactOnDeliverables());
    assertEquals("New Plan", savedRequest.getPlanDuringExtension());
    assertEquals(detail, savedRequest.getScheduleWorkProgrammeApplicationDetails());
    assertNotNull(savedRequest);
  }

  @Test
  void getLicenceScheduleRequestForm_foundExistingRequest() {
    ScheduleWorkProgrammeApplicationDetail detail = new ScheduleWorkProgrammeApplicationDetail();
    LicenceScheduleSupportingRequest existingRequest = new LicenceScheduleSupportingRequest();
    existingRequest.setLicenceProgress("Progress Info");
    existingRequest.setReasonForAmendment("Reason");
    existingRequest.setImpactOnDeliverables("Impact");
    existingRequest.setPlanDuringExtension("Plan");

    when(licenceScheduleSupportingRequestRepository.findByScheduleWorkProgrammeApplicationDetails(detail))
        .thenReturn(Optional.of(existingRequest));

    LicenceScheduleSupportingRequestForm resultForm = licenceScheduleSupportingRequestService
        .getLicenceScheduleRequestForm(detail);

    assertNotNull(resultForm);
    assertEquals("Progress Info", resultForm.getLicenceProgress());
    assertEquals("Reason", resultForm.getReasonForAmendment());
    assertEquals("Impact", resultForm.getImpactOnDeliverables());
    assertEquals("Plan", resultForm.getPlanDuringExtension());
  }

  @Test
  void getLicenceScheduleRequestForm_notFound() {
    ScheduleWorkProgrammeApplicationDetail detail = new ScheduleWorkProgrammeApplicationDetail();

    when(licenceScheduleSupportingRequestRepository.findByScheduleWorkProgrammeApplicationDetails(detail))
        .thenReturn(Optional.empty());

    LicenceScheduleSupportingRequestForm resultForm = licenceScheduleSupportingRequestService
        .getLicenceScheduleRequestForm(detail);

    assertNotNull(resultForm);
    verify(licenceScheduleSupportingRequestRepository).findByScheduleWorkProgrammeApplicationDetails(detail);
    verify(licenceScheduleSupportingRequestRepository, never()).save(any());
  }
}