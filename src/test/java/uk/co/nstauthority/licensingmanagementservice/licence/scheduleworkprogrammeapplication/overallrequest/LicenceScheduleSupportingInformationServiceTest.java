package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileFormTestUtil;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleSupportingInformationServiceTest {

  @Mock
  private LicenceScheduleSupportingInformationRepository licenceScheduleSupportingInformationRepository;

  @Mock
  private LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService;

  @InjectMocks
  private LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;

  @Captor
  private ArgumentCaptor<LicenceScheduleSupportingInformation> licenceScheduleSupportingRequestArgumentCaptor;

  @Mock
  private ApplicationFileService licenceScheduleApplicationFileService;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());
  }

  @Test
  void saveRequestForm_existingOverallRequest() {
    LicenceScheduleSupportingInformation existingRequest = new LicenceScheduleSupportingInformation();
    existingRequest.setLicenceProgress("Old Info");

    LicenceScheduleSupportingInformationForm form = new LicenceScheduleSupportingInformationForm();
    form.setLicenceProgress("New Progress Info");
    form.setReasonForAmendment("New Reason");
    form.setImpactOnDeliverables("New Impact");
    form.setPlanDuringExtension("New Plan");
    form.setDocuments(List.of(UploadedFileFormTestUtil.newBuilder().build()));

    when(licenceScheduleSupportingInformationRepository.findByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(existingRequest));

    licenceScheduleSupportingInformationService.saveRequestForm(form, scheduleWorkProgrammeApplicationDetail);

    verify(licenceScheduleSupportingInformationRepository).save(licenceScheduleSupportingRequestArgumentCaptor.capture());

    verify(licenceScheduleApplicationFileService).saveDocuments(
        LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail),
        form.getDocuments()
    );

    LicenceScheduleSupportingInformation savedRequest = licenceScheduleSupportingRequestArgumentCaptor.getValue();
    assertEquals("New Progress Info", savedRequest.getLicenceProgress());
    assertEquals("New Reason", savedRequest.getReasonForAmendment());
    assertEquals("New Impact", savedRequest.getImpactOnDeliverables());
    assertEquals("New Plan", savedRequest.getPlanDuringExtension());
    assertEquals(scheduleWorkProgrammeApplicationDetail, savedRequest.getScheduleWorkProgrammeApplicationDetails());
    assertEquals(existingRequest, savedRequest);
  }

  @Test
  void saveRequestForm_newOverallRequest() {
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());

    LicenceScheduleSupportingInformationForm form = new LicenceScheduleSupportingInformationForm();
    form.setLicenceProgress("New Progress Info");
    form.setReasonForAmendment("New Reason");
    form.setImpactOnDeliverables("New Impact");
    form.setPlanDuringExtension("New Plan");
    form.setDocuments(List.of(UploadedFileFormTestUtil.newBuilder().build()));

    when(licenceScheduleSupportingInformationRepository.findByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.empty());

    licenceScheduleSupportingInformationService.saveRequestForm(form, scheduleWorkProgrammeApplicationDetail);

    verify(licenceScheduleApplicationFileService).saveDocuments(
        LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail),
        form.getDocuments()
    );

    verify(licenceScheduleSupportingInformationRepository).save(licenceScheduleSupportingRequestArgumentCaptor.capture());

    LicenceScheduleSupportingInformation savedRequest = licenceScheduleSupportingRequestArgumentCaptor.getValue();
    assertEquals("New Progress Info", savedRequest.getLicenceProgress());
    assertEquals("New Reason", savedRequest.getReasonForAmendment());
    assertEquals("New Impact", savedRequest.getImpactOnDeliverables());
    assertEquals("New Plan", savedRequest.getPlanDuringExtension());
    assertEquals(scheduleWorkProgrammeApplicationDetail, savedRequest.getScheduleWorkProgrammeApplicationDetails());
    assertNotNull(savedRequest);
  }

  @Test
  void getLicenceScheduleRequestForm_foundExistingRequest() {
    LicenceScheduleSupportingInformation existingRequest = new LicenceScheduleSupportingInformation();
    existingRequest.setLicenceProgress("Progress Info");
    existingRequest.setReasonForAmendment("Reason");
    existingRequest.setImpactOnDeliverables("Impact");
    existingRequest.setPlanDuringExtension("Plan");

    when(licenceScheduleSupportingInformationRepository.findByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(existingRequest));

    LicenceScheduleSupportingInformationForm resultForm = licenceScheduleSupportingInformationService
        .getLicenceScheduleRequestForm(scheduleWorkProgrammeApplicationDetail);

    assertNotNull(resultForm);
    assertEquals("Progress Info", resultForm.getLicenceProgress());
    assertEquals("Reason", resultForm.getReasonForAmendment());
    assertEquals("Impact", resultForm.getImpactOnDeliverables());
    assertEquals("Plan", resultForm.getPlanDuringExtension());
  }

  @Test
  void getLicenceScheduleRequestForm_notFound() {
    when(licenceScheduleSupportingInformationRepository.findByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.empty());

    LicenceScheduleSupportingInformationForm resultForm = licenceScheduleSupportingInformationService
        .getLicenceScheduleRequestForm(scheduleWorkProgrammeApplicationDetail);

    assertNotNull(resultForm);
    verify(licenceScheduleSupportingInformationRepository).findByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    verify(licenceScheduleSupportingInformationRepository, never()).save(any());
  }

  @Test
  void handleSupportingInformationExtensionRemoval_NeitherExtensionNorAmendmentExists() {
    when(licenceScheduleSupportingInformationHelperService.isExtensionOrAmendment(scheduleWorkProgrammeApplicationDetail)).thenReturn(false);

    LicenceScheduleSupportingInformation request = mock(LicenceScheduleSupportingInformation.class);

    when(licenceScheduleSupportingInformationService.getRequestByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(request));

    licenceScheduleSupportingInformationService.handleSupportingInformationExtensionRemoval(scheduleWorkProgrammeApplicationDetail);

    verify(request).setPlanDuringExtension(null);
    verify(licenceScheduleSupportingInformationRepository, times(1)).save(request);
  }
}