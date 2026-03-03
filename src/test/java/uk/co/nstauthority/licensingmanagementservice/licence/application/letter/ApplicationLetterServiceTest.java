package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.instance.DocumentInstanceSectionDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@ExtendWith(MockitoExtension.class)
class ApplicationLetterServiceTest {

  @Mock
  private DocumentInstanceService documentInstanceService;

  @Mock
  private DocumentInstanceSectionService documentInstanceSectionService;

  @Mock
  private DocumentTemplateService documentTemplateService;

  @Mock
  private LicenceApplication application;

  @InjectMocks
  private ApplicationLetterService applicationLetterService;

  @Test
  void getOrCreateDocumentInstance_whenExists_returnsExisting() {
    when(application.getId()).thenReturn(UUID.randomUUID());

    var existingDoc = DocumentInstanceDtoTestUtil.newBuilder().build();
    when(documentInstanceService.getDocumentInstanceDtosByItemReference(application.getId().toString()))
        .thenReturn(List.of(existingDoc));

    var result = applicationLetterService.getOrCreateDocumentInstance(application);
    assertThat(result).isEqualTo(existingDoc);
  }

  @Test
  void getOrCreateDocumentInstance_whenNotExists_createsNew() {
    when(application.getId()).thenReturn(UUID.randomUUID());
    when(application.getApplicationType()).thenReturn(ApplicationType.CONTINUATION_APPLICATION);

    when(documentInstanceService.getDocumentInstanceDtosByItemReference(application.getId().toString()))
        .thenReturn(List.of());

    var template = DocumentTemplateDtoTestUtil
        .newBuilder()
        .withTitle("Title")
        .withDescription("Desc")
        .build();

    when(documentTemplateService.getDocumentTemplateDtoByMnemonicOrThrow("CONTINUATION_LETTER-CONTINUATION_APPLICATION"))
        .thenReturn(template);

    var newDoc = DocumentInstanceDtoTestUtil.newBuilder().build();
    when(documentInstanceService.createDocumentInstance(
        application.getId().toString(),
        "CONTINUATION_APPLICATION",
        template.title(),
        template.description(),
        template
    )).thenReturn(newDoc);

    var result = applicationLetterService.getOrCreateDocumentInstance(application);
    assertThat(result).isEqualTo(newDoc);
  }

  @Test
  void getApplicationLetters_whenNoApplications_thenReturnEmptyList() {
    assertThat(applicationLetterService.getApplicationLetters(List.of())).isEmpty();
  }

  @Test
  void getApplicationLetterTitle_whenFound() {
    when(application.getId()).thenReturn(UUID.randomUUID());

    var letter = DocumentInstanceDtoTestUtil.newBuilder().withTitle("Expected Title").build();
    when(documentInstanceService.getDocumentInstanceDtosByItemReference(application.getId().toString()))
        .thenReturn(List.of(letter));

    assertThat(applicationLetterService.getApplicationLetterTitle(application)).isEqualTo("Expected Title");
  }

  @Test
  void getApplicationLetterTitle_whenNotFound_throwsException() {
    when(application.getId()).thenReturn(UUID.randomUUID());

    when(documentInstanceService.getDocumentInstanceDtosByItemReference(application.getId().toString()))
        .thenReturn(List.of());

    assertThatThrownBy(() -> applicationLetterService.getApplicationLetterTitle(application))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("No document instance found for application");
  }

  @ParameterizedTest
  @EnumSource(value=AddSectionOption.class, names = {"ADD_BEFORE", "ADD_AFTER"}, mode= EnumSource.Mode.INCLUDE)
  void getParentDocumentSectionDto_withParentId(AddSectionOption addSectionOption) {
    var parentDocumentSectionDto = DocumentInstanceSectionDtoTestUtil.newBuilder().build();
    var documentSectionDto = DocumentInstanceSectionDtoTestUtil
        .newBuilder()
        .withParentId(parentDocumentSectionDto.id())
        .build();

    when(documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(parentDocumentSectionDto.id()))
        .thenReturn(parentDocumentSectionDto);

    assertThat(applicationLetterService.getParentDocumentSectionDto(addSectionOption, documentSectionDto))
        .isEqualTo(parentDocumentSectionDto);
  }

  @Test
  void getApplicationLetters_whenApplicationsExist_returnsDocumentInstances() {
    var applicationId = UUID.randomUUID();

    when(application.getId()).thenReturn(applicationId);

    var applicationIds = List.of(applicationId.toString());
    var applications = List.of(application);

    var doc1 = DocumentInstanceDtoTestUtil.newBuilder().build();
    var doc2 = DocumentInstanceDtoTestUtil.newBuilder().build();
    var expectedDocs = List.of(doc1, doc2);

    when(documentInstanceService.getDocumentInstanceDtosByItemReferences(applicationIds))
        .thenReturn(expectedDocs);

    var result = applicationLetterService.getApplicationLetters(applications);

    assertThat(result).containsExactlyElementsOf(expectedDocs);
  }

  @Test
  void getApplicationLetters_whenApplicationsNull_thenReturnEmptyList() {
    assertThat(applicationLetterService.getApplicationLetters(null)).isEmpty();
  }
}