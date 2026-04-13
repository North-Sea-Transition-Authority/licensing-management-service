package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMetadata;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMetadataService;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@ExtendWith(MockitoExtension.class)
class NextTermPhaseStartDateMailMergeFieldTest {

  @Mock
  private DocumentLinkingService linkingService;

  @Mock
  private DocumentTemplateMetadataService documentTemplateMetadataService;

  @InjectMocks
  private NextTermPhaseStartDateMailMergeField mailMergeField;

  @Test
  void getMnemonic() {
    assertThat(mailMergeField.getMnemonic()).isEqualTo(NextTermPhaseStartDateMailMergeField.MNEMONIC);
  }

  @Test
  void getDescription() {
    assertThat(mailMergeField.getDescription()).isEqualTo(NextTermPhaseStartDateMailMergeField.DESCRIPTION);
  }

  @Test
  void isApplicable_whenContinuationApplication_returnsTrue() {
    var documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();
    var metadata = new DocumentTemplateMetadata();
    metadata.setApplicationType(ApplicationType.CONTINUATION_APPLICATION);

    when(documentTemplateMetadataService.getDocumentTemplateMetadata(documentTemplateDto.id()))
        .thenReturn(Optional.of(metadata));

    assertThat(mailMergeField.isApplicable(documentTemplateDto)).isTrue();
  }

  @Test
  void isApplicable_whenNotContinuationApplication_returnsFalse() {
    var documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();
    var metadata = new DocumentTemplateMetadata();
    metadata.setApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);

    when(documentTemplateMetadataService.getDocumentTemplateMetadata(documentTemplateDto.id()))
        .thenReturn(Optional.of(metadata));

    assertThat(mailMergeField.isApplicable(documentTemplateDto)).isFalse();
  }

  @Test
  void isApplicable_whenMetadataNotFound_returnsFalse() {
    var documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();

    when(documentTemplateMetadataService.getDocumentTemplateMetadata(documentTemplateDto.id()))
        .thenReturn(Optional.empty());

    assertThat(mailMergeField.isApplicable(documentTemplateDto)).isFalse();
  }

  @Test
  void resolve() {
    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(UUID.randomUUID().toString())
        .build();

    var startDate = "01 Jan 2026";

    when(linkingService.getNextTermPhaseStartDateFromDto(documentInstanceDto)).thenReturn(startDate);

    assertThat(mailMergeField.resolve(documentInstanceDto))
        .isEqualTo(DocumentMailMergeFieldResolveResult.successNoEsc(startDate));
  }
}
