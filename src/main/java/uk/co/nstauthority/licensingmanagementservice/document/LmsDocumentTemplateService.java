package uk.co.nstauthority.licensingmanagementservice.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldFormatter;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionUrls;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionsSummaryView;

@Service
public class LmsDocumentTemplateService {

  private final DocumentTemplateSectionViewService documentTemplateSectionViewService;

  @Autowired
  public LmsDocumentTemplateService(
      DocumentTemplateSectionViewService documentTemplateSectionViewService
  ) {
    this.documentTemplateSectionViewService = documentTemplateSectionViewService;
  }

  public DocumentTemplateSectionsSummaryView getDocumentTemplateSectionsSummaryView(
      DocumentTemplateDto documentTemplateDto,
      DocumentMailMergeFieldFormatter mailMergeFormatter
  ) {
    return documentTemplateSectionViewService.getDocumentTemplateSectionsSummaryView(
        documentTemplateDto,
        this::getDocumentSectionUrls,
        mailMergeFormatter
    );
  }

  //TODO add real world section urls
  DocumentTemplateSectionUrls getDocumentSectionUrls(DocumentTemplateSectionDto documentTemplateSectionDto) {
    return new DocumentTemplateSectionUrls("", "", "", "", "");
  }
}