package uk.co.nstauthority.licensingmanagementservice.document;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.fivium.digitaldocumentlibrary.document.FreeMarkerTemplateRenderingService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@ExtendWith(MockitoExtension.class)
class DocumentGenerationServiceTest {

  @Mock
  private DocumentTemplateService documentTemplateService;

  @Mock
  private DocumentTemplateMetadataService documentTemplateMetadataService;

  @Mock
  private FreeMarkerTemplateRenderingService freeMarkerTemplateRenderingService;

  @Mock
  private DocumentTemplateSectionService documentTemplateSectionService;

  @Mock
  private DocumentTemplateProvider mockProvider;

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
  }

  @InjectMocks
  private DocumentGenerationService documentGenerationService;

  @Test
  void createTemplates_allTemplates() {

    Map<String, DocumentTemplate> mnemonicToTitleMap = templates
        .stream()
        .collect(Collectors.toMap(DocumentTemplate::getMnemonic, Function.identity()));

    var mnemonicToDocumentDtoMap = mockAndReturnTemplates(mnemonicToTitleMap);

    documentGenerationService.createTemplates(templates);

    for (var template : templates) {
      var expectedSections = mnemonicToTitleMap
          .get(template.getMnemonic())
          .getDocumentSections();

      verify(documentTemplateMetadataService).createDocumentMetadata(
          mnemonicToDocumentDtoMap.get(template.getMnemonic()).id(),
          mnemonicToTitleMap.get(template.getMnemonic()).getLicenceType(),
          template.getApplicationType()
      );

      expectedSections.forEach(section -> verify(documentTemplateSectionService).createDocumentTemplateSection(
          mnemonicToDocumentDtoMap.get(template.getMnemonic()),
          section.parentDto(),
          section.title(),
          section.content(),
          section.conditionMnemonic(),
          section.isNumbered(),
          section.hasPageBreakBefore(),
          section.displayOrder()
      ));
    }
  }

  private void mockFreemarkerContent(List<DocumentTemplateSection> expectedSections) {
    expectedSections.forEach(section -> {
      if ((section.contentFreemarker()) != null) {
        try {
          when(freeMarkerTemplateRenderingService.renderTemplate(section.contentFreemarker(), Map.of()))
              .thenReturn(section.content());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private Map<String, DocumentTemplateDto> mockAndReturnTemplates(Map<String, DocumentTemplate> templateByMnemonic) {
    Map<String, DocumentTemplateDto> templateDtoMap = new HashMap<>();

    for (var template : templates) {
      var documentTemplate = DocumentTemplateDtoTestUtil.newBuilder().withMnemonic(template.getMnemonic()).build();

      when(documentTemplateService.createDocumentTemplate(
          template.getMnemonic(),
          template.getType().getTitle(),
          template.getType().getDescription(),
          template.getType().getDocumentInstancePdfTemplatePath(),
          template.getDisplayOrder()))
          .thenReturn(documentTemplate);

      templateDtoMap.put(template.getMnemonic(), documentTemplate);

      mockFreemarkerContent(templateByMnemonic.get(template.getMnemonic()).getDocumentSections());
    }

    return templateDtoMap;
  }
}