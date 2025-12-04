package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType.EMPTY_SUMMARY;
import static uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType.FILES_SUMMARY;
import static uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType.SIMPLE_SUMMARY;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformation;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationHelperService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;

@ExtendWith(MockitoExtension.class)
class SupportingInformationSummarySectionServiceTest {

  @Mock
  private LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;

  @Mock
  private LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService;

  @Mock
  private FileService fileService;

  @InjectMocks
  private SupportingInformationSummarySectionService supportingInformationSummarySectionService;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setId(UUID.randomUUID());
  }

  @Test
  void getSummarySection_withDocument() {
    var supportingInfo = new LicenceScheduleSupportingInformation();
    when(licenceScheduleSupportingInformationService.getRequestByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(supportingInfo));

    when(fileService.findAll(any(), any(), any())).thenReturn(Collections.emptyList());

    var summarySection = supportingInformationSummarySectionService.getSummarySection(scheduleWorkProgrammeApplicationDetail, null);

    assertThat(summarySection).isPresent();
    assertThat(summarySection.get().summaryItems().get(0).displayName()).isEqualTo(SupportingInformationSummarySectionService.SUPPORTING_INFORMATION_SUMMARY);
    assertThat(summarySection.get().summaryItems().get(1).displayName()).isEqualTo("Supporting information documents");
  }

  @Test
  void getSupportingDocumentsSummaryItem_filesFound() {
    UploadedFile uploadedFile = new UploadedFile(UUID.randomUUID());
    uploadedFile.setName("test.pdf");
    uploadedFile.setKey("key");

    when(fileService.findAll(any(), any(), any())).thenReturn(List.of(uploadedFile));

    SummaryItem supportingDocumentsSummaryItem = supportingInformationSummarySectionService.getSupportingDocumentsSummaryItem(scheduleWorkProgrammeApplicationDetail);
    SummaryCard summaryCard = supportingDocumentsSummaryItem.summaryCards().getFirst();
    List<SummaryFileView> fileViews = (List<SummaryFileView>) summaryCard.summaryData();

    assertEquals(1, fileViews.size());
    assertEquals(1, supportingDocumentsSummaryItem.summaryCards().size());
    assertEquals(FILES_SUMMARY, summaryCard.summaryCardType());
    assertEquals("test.pdf", fileViews.getFirst().uploadedFileViews().getFirst().fileName());
  }

  @Test
  void getSummaryItem_withAllSupportingInformation() {
    var supportingInfo = new LicenceScheduleSupportingInformation();
    supportingInfo.setReasonForAmendment("Reason for amendment data");
    supportingInfo.setLicenceProgress("Progress details data");
    supportingInfo.setImpactOnDeliverables("Impact details data");
    supportingInfo.setPlanDuringExtension("Plan during extension data");

    when(licenceScheduleSupportingInformationService.getRequestByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(supportingInfo));

    when(licenceScheduleSupportingInformationHelperService.isExtensionOrAmendment(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(true);

    var summaryItem = supportingInformationSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        SupportingInformationSummarySectionService.SUPPORTING_INFORMATION_SUMMARY
    );

    var card = summaryItem.summaryCards().getFirst();

    assertThat(summaryItem.summaryCards()).hasSize(1);
    assertThat(card.summaryCardType()).isEqualTo(SIMPLE_SUMMARY);

    var keyValues = (List<?>) ReflectionTestUtils.getField(card.summaryData(), "keyValues");
    assertThat(keyValues)
        .hasSize(4)
        .extracting(Object::toString)
        .anyMatch(summaryData -> summaryData.contains("Reason for request") && summaryData.contains("Reason for amendment data"))
        .anyMatch(summaryData -> summaryData.contains("Licence progress") && summaryData.contains("Progress details data"))
        .anyMatch(summaryData -> summaryData.contains("Impact on deliverables") && summaryData.contains("Impact details data"))
        .anyMatch(summaryData -> summaryData.contains("Plan during extension") && summaryData.contains("Plan during extension data"));
  }

  @Test
  void getSummaryItem_withEmptySupportingInformation() {
    var summaryItem = supportingInformationSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        SupportingInformationSummarySectionService.SUPPORTING_INFORMATION_SUMMARY
    );

    assertThat(summaryItem.summaryCards().getFirst().summaryCardType()).isEqualTo(EMPTY_SUMMARY);
    assertThat(summaryItem.summaryCards().getFirst().summaryData()).isNull();
    assertThat(summaryItem.summaryCards().getFirst().displayName()).isNull();
  }

  @Test
  void getSummaryItem_withPartialSupportingInformation() {
    var supportingInfo = new LicenceScheduleSupportingInformation();
    supportingInfo.setReasonForAmendment("Only reason provided data");

    when(licenceScheduleSupportingInformationService.getRequestByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(supportingInfo));

    when(licenceScheduleSupportingInformationHelperService.isExtensionOrAmendment(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(false);

    var summaryItem = supportingInformationSummarySectionService.getLicenceSummaryItem(scheduleWorkProgrammeApplicationDetail, SupportingInformationSummarySectionService.SUPPORTING_INFORMATION_SUMMARY);
    var summaryCard = summaryItem.summaryCards().getFirst();

    assertThat(summaryItem.summaryCards()).hasSize(1);

    var keyValues = (List<?>) ReflectionTestUtils.getField(summaryCard.summaryData(), "keyValues");
    assertThat(keyValues)
        .hasSize(3)
        .extracting(Object::toString)
        .anyMatch(summaryData -> summaryData.contains("Reason for request") && summaryData.contains("Only reason provided data"))
        .noneMatch(summaryData -> summaryData.contains("Plan during extension"));
  }
}