package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;

@Service
class DocumentTemplateBootstrapService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentTemplateBootstrapService.class);

  private final DocumentTemplateService documentTemplateService;
  private final DocumentGenerationService documentGenerationService;
  private final List<DocumentTemplateProvider> templateProviders;

  @Autowired
  DocumentTemplateBootstrapService(
      DocumentTemplateService documentTemplateService,
      DocumentGenerationService documentGenerationService,
      List<DocumentTemplateProvider> templateProviders
  ) {
    this.documentTemplateService = documentTemplateService;
    this.documentGenerationService = documentGenerationService;
    this.templateProviders = templateProviders;
  }

  @EventListener(ApplicationReadyEvent.class)
  void onApplicationReadyEvent() {
    List<DocumentTemplate> allTemplates = templateProviders.stream()
        .map(DocumentTemplateProvider::getTemplate)
        .toList();

    var documentTemplates = documentTemplateService.getDocumentTemplateDtos();
    if (CollectionUtils.isEmpty(documentTemplates)) {
      LOGGER.info("Creating initial document templates");
      documentGenerationService.createTemplates(allTemplates);
      return;
    }

    var allTemplateMnemonics = allTemplates.stream()
        .map(DocumentTemplate::getMnemonic)
        .toList();

    var templateMnemonics = documentTemplates.stream()
        .map(DocumentTemplateDto::mnemonic)
        .toList();

    if (!CollectionUtils.containsAll(templateMnemonics, allTemplateMnemonics)) {
      var missingTemplates = allTemplates.stream()
          .filter(documentTemplate -> !templateMnemonics.contains(documentTemplate.getMnemonic()))
          .toList();
      LOGGER.info("Creating missing document templates");
      documentGenerationService.createTemplates(missingTemplates);
      return;
    }

    LOGGER.info("Found existing document templates, not creating initial document templates");
  }
}