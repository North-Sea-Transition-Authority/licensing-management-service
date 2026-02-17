package uk.co.nstauthority.licensingmanagementservice.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@ExtendWith(MockitoExtension.class)
class DocumentTemplateBootstrapServiceTest {

  @Mock
  private DocumentTemplateService documentTemplateService;

  @Mock
  private DocumentGenerationService documentGenerationService;

  @Mock
  private DocumentTemplateProvider mockProvider;

  private DocumentTemplateBootstrapService documentTemplateBootstrapService;

  private List<DocumentTemplate> templates;

  @BeforeEach
  void setUp() {
    var dummyTemplate = DocumentTemplate.newBuilder()
        .withTemplate(DocumentTemplateType.CONTINUATION_LETTER)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withApplicationType(ApplicationType.CONTINUATION_APPLICATION)
        .withDisplayOrder(10)
        .build();

    templates = List.of(dummyTemplate);

    when(mockProvider.getTemplate()).thenReturn(dummyTemplate);

    documentTemplateBootstrapService = new DocumentTemplateBootstrapService(
        documentTemplateService,
        documentGenerationService,
        List.of(mockProvider)
    );
  }
  @Test
  void onApplicationReadyEvent_whenNoTemplatesExist() {
    when(documentTemplateService.getDocumentTemplateDtos()).thenReturn(List.of());
    documentTemplateBootstrapService.onApplicationReadyEvent();
    verify(documentGenerationService).createTemplates(templates);
  }

  @Test
  void onApplicationReadyEvent_whenAllTemplatesExist() {
    var documents = new ArrayList<DocumentTemplateDto>();
    templates.forEach(documentTemplateType -> {
      var transactionDocument = DocumentTemplateDtoTestUtil.newBuilder()
          .withMnemonic(documentTemplateType.getMnemonic())
          .build();
      documents.add(transactionDocument);
    });

    when(documentTemplateService.getDocumentTemplateDtos()).thenReturn(documents);
    documentTemplateBootstrapService.onApplicationReadyEvent();
    verify(documentGenerationService, never()).createTemplates(any());
  }

  @Test
  void onApplicationReadyEvent_whenSomeTemplatesExist() {
    var existingMockTemplateDto = DocumentTemplateDtoTestUtil.newBuilder()
        .withMnemonic("SOME_OTHER_TEMPLATE-SEAWARD_PRODUCTION")
        .build();

    var expectedTemplatesToAdd = templates
        .stream()
        .filter(documentTemplate -> !documentTemplate.getMnemonic().equals(existingMockTemplateDto.mnemonic()))
        .toList();

    when(documentTemplateService.getDocumentTemplateDtos()).thenReturn(List.of(existingMockTemplateDto));
    documentTemplateBootstrapService.onApplicationReadyEvent();
    verify(documentGenerationService).createTemplates(expectedTemplatesToAdd);
  }
}