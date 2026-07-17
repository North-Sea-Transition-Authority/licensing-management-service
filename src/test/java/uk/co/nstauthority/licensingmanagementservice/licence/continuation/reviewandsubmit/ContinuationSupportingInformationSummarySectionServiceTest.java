package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformation;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationFileUsages;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType;

@ExtendWith(MockitoExtension.class)
class ContinuationSupportingInformationSummarySectionServiceTest {

  @Mock
  private LicenceContinuationSupportingInformationService licenceContinuationSupportingInformationService;

  @Mock
  private FileService fileService;

  @InjectMocks
  private ContinuationSupportingInformationSummarySectionService summarySectionService;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private LicenceContinuationSupportingInformationFileUsages fileUsages;
  private ServiceUserDetail user;

  @BeforeEach
  void setUp() {
    licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    licenceContinuationApplicationDetail.setId(UUID.randomUUID());
    fileUsages = LicenceContinuationSupportingInformationFileUsages.fromApplication(licenceContinuationApplicationDetail);
    user = mock(ServiceUserDetail.class);
  }

  @Test
  void getSummarySection_whenNoDocuments_assertAnswerCard() {
    var supportingInformation = new LicenceContinuationSupportingInformation();
    supportingInformation.setHasAdditionalSupportingInformation(false);

    when(fileService.findAll(fileUsages.usageId(), fileUsages.usageType(), fileUsages.documentType()))
        .thenReturn(Collections.emptyList());
    when(licenceContinuationSupportingInformationService.getSupportingInformation(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(supportingInformation));

    var result = summarySectionService.getSummarySection(licenceContinuationApplicationDetail, user);

    assertThat(result).isPresent();
    var summarySection = result.get();
    assertThat(summarySection.displayOrder())
        .isEqualTo(ContinuationSupportingInformationSummarySectionService.SECTION_DISPLAY_ORDER);

    var summaryItem = summarySection.summaryItems().getFirst();
    assertThat(summaryItem.displayName())
        .isEqualTo(ContinuationSupportingInformationSummarySectionService.SECTION_NAME);
    assertThat(summaryItem.summaryCards()).hasSize(1);
    assertThat(summaryItem.summaryCards().getFirst().summaryCardType()).isEqualTo(SummaryCardType.SIMPLE_SUMMARY);
  }

  @Test
  void getSummarySection_whenDocumentsUploaded_assertFilesCard() {
    var uploadedFile = new UploadedFile(UUID.randomUUID());
    uploadedFile.setName("supporting-info.pdf");
    uploadedFile.setKey("key");

    when(fileService.findAll(fileUsages.usageId(), fileUsages.usageType(), fileUsages.documentType()))
        .thenReturn(List.of(uploadedFile));
    when(licenceContinuationSupportingInformationService.getSupportingInformation(licenceContinuationApplicationDetail))
        .thenReturn(Optional.empty());

    var result = summarySectionService.getSummarySection(licenceContinuationApplicationDetail, user);

    assertThat(result).isPresent();
    var summaryItem = result.get().summaryItems().getFirst();
    assertThat(summaryItem.summaryCards()).hasSize(1);
    assertThat(summaryItem.summaryCards().getFirst().summaryCardType()).isEqualTo(SummaryCardType.FILES_SUMMARY);
  }
}
