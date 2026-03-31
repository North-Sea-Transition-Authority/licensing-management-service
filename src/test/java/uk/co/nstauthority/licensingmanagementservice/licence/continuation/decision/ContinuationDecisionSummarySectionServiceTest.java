package uk.co.nstauthority.licensingmanagementservice.licence.continuation.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType.FILES_AND_DETAILS_SUMMARY;

import java.time.Instant;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileAndDetailsView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class ContinuationDecisionSummarySectionServiceTest {

  @Mock
  private FileService fileService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @InjectMocks
  private ContinuationDecisionSummarySectionService service;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private LicenceContinuationApplication licenceContinuationApplication;

  @BeforeEach
  void setUp() {
    licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setId(UUID.randomUUID());

    licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withLicenceContinuationApplication(licenceContinuationApplication)
        .withId(UUID.randomUUID()).build();

  }

  @Test
  void getSummarySection_whenNoFilesFound() {
    when(fileService.findAll(any(), any(), any())).thenReturn(Collections.emptyList());

    Optional<SummarySection> result = service.getSummarySection(licenceContinuationApplicationDetail);

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenFilesFoundAndUserExists() {
    UploadedFile uploadedFile = new UploadedFile(UUID.randomUUID());
    uploadedFile.setName("decision-letter.pdf");
    uploadedFile.setKey("testkey");
    uploadedFile.setUploadedBy("101");
    uploadedFile.setUploadedAt(Instant.parse("2025-01-01T12:00:00Z"));

    when(fileService.findAll(any(), any(), any())).thenReturn(List.of(uploadedFile));

    var submittedByUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(101L)
        .withForename("test")
        .withSurname("user")
        .buildJson();

    when(energyPortalUserService.findByWuaId(eq(WebUserAccountId.from(101L)), any()))
        .thenReturn(Optional.of(submittedByUser));

    Optional<SummarySection> result = service.getSummarySection(licenceContinuationApplicationDetail);

    assertThat(result).isPresent();
    SummarySection summarySection = result.get();

    assertThat(summarySection.displayOrder())
        .isEqualTo(ContinuationDecisionSummarySectionService.SECTION_DISPLAY_ORDER);

    SummaryItem summaryItem = summarySection.summaryItems().getFirst();
    assertThat(summaryItem.displayName())
        .isEqualTo(ContinuationDecisionSummarySectionService.CONTINUATION_LETTER_DETAILS);

    SummaryCard summaryCard = summaryItem.summaryCards().getFirst();
    assertThat(summaryCard.summaryCardType()).isEqualTo(FILES_AND_DETAILS_SUMMARY);

    SummaryFileAndDetailsView mixedData = (SummaryFileAndDetailsView) summaryCard.summaryData();

    assertThat(mixedData.summaryData().keyValues())
        .hasSize(2)
        .extracting(Object::toString)
        .anyMatch(data -> data.contains("test user"))
        .anyMatch(data -> data.contains("1 January 2025 12:00:00"));

    assertThat(mixedData.fileViews()).hasSize(1);
    assertThat(mixedData.fileViews().getFirst().uploadedFileViews().getFirst().fileName())
        .isEqualTo("decision-letter.pdf");
  }

  @Test
  void getSummarySection_whenUserNotFound() {
    UploadedFile uploadedFile = new UploadedFile(UUID.randomUUID());
    uploadedFile.setUploadedBy("1");
    uploadedFile.setKey("testkey");

    when(fileService.findAll(any(), any(), any())).thenReturn(List.of(uploadedFile));

    when(energyPortalUserService.findByWuaId(any(), any())).thenReturn(Optional.empty());

    Optional<SummarySection> result = service.getSummarySection(licenceContinuationApplicationDetail);

    assertThat(result).isPresent();
    SummaryCard summaryCard = result.get().summaryItems().getFirst().summaryCards().getFirst();
    SummaryFileAndDetailsView mixedData = (SummaryFileAndDetailsView) summaryCard.summaryData();

    assertThat(mixedData.summaryData().keyValues())
        .extracting(Object::toString)
        .anyMatch(data -> data.contains("Not allocated"));
  }

}