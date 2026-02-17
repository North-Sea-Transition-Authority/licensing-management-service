package uk.co.nstauthority.licensingmanagementservice.document;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.fivium.digitaldocumentlibrary.document.FreeMarkerTemplateRenderingService;

@Service
public class DocumentGenerationService {

  private final DocumentTemplateService documentTemplateService;
  private final DocumentTemplateMetadataService documentTemplateMetadataService;
  private final FreeMarkerTemplateRenderingService freeMarkerTemplateRenderingService;
  private final DocumentTemplateSectionService documentTemplateSectionService;

  @Autowired
  public DocumentGenerationService(
      DocumentTemplateService documentTemplateService,
      DocumentTemplateMetadataService documentTemplateMetadataService,
      FreeMarkerTemplateRenderingService freeMarkerTemplateRenderingService,
      DocumentTemplateSectionService documentTemplateSectionService
  ) {
    this.documentTemplateService = documentTemplateService;
    this.documentTemplateMetadataService = documentTemplateMetadataService;
    this.freeMarkerTemplateRenderingService = freeMarkerTemplateRenderingService;
    this.documentTemplateSectionService = documentTemplateSectionService;
  }

  @Transactional
  void createTemplates(List<DocumentTemplate> templatesToAdd) {
    templatesToAdd.forEach(template -> {
      var templateDto = documentTemplateService.createDocumentTemplate(
          template.getMnemonic(),
          template.getType().getTitle(),
          template.getType().getDescription(),
          template.getType().getDocumentInstancePdfTemplatePath(),
          template.getDisplayOrder()
      );

      documentTemplateMetadataService.createDocumentMetadata(
          templateDto.id(),
          template.getLicenceType(),
          template.getApplicationType()
      );

      template.getDocumentSections().forEach(section -> {
        String content;
        try {
          content = ((section.contentFreemarker()) != null)
              ? freeMarkerTemplateRenderingService.renderTemplate(section.contentFreemarker(), Map.of())
              : section.content();
        } catch (Exception e) {
          throw new IllegalStateException(
              "Failed to render content for section '" + section.title() + "' in template '" + template.getMnemonic() + "'.", e
          );
        }

        documentTemplateSectionService.createDocumentTemplateSection(
            templateDto,
            section.parentDto(),
            section.title(),
            content,
            section.conditionMnemonic(),
            section.isNumbered(),
            section.hasPageBreakBefore(),
            section.displayOrder()
        );
      });
    });
  }
}
