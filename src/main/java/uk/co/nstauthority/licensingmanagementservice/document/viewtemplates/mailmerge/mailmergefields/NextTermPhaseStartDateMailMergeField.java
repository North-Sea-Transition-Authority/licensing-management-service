package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeField;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMetadataService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@Order(DocumentMailMergeFieldDisplayOrders.NEXT_TERM_PHASE_START_DATE)
@Component
public class NextTermPhaseStartDateMailMergeField implements DocumentMailMergeField {

  static final String MNEMONIC = "NEXT_TERM_PHASE_START_DATE";
  static final String DESCRIPTION = "The start date of the next term or phase of the licence";

  private final DocumentLinkingService documentLinkingService;
  private final DocumentTemplateMetadataService documentTemplateMetadataService;

  @Autowired
  public NextTermPhaseStartDateMailMergeField(
      DocumentLinkingService documentLinkingService,
      DocumentTemplateMetadataService documentTemplateMetadataService
  ) {
    this.documentLinkingService = documentLinkingService;
    this.documentTemplateMetadataService = documentTemplateMetadataService;
  }

  @Override
  public String getMnemonic() {
    return MNEMONIC;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public boolean isApplicable(DocumentTemplateDto documentTemplateDto) {
    return documentTemplateMetadataService.getDocumentTemplateMetadata(documentTemplateDto.id())
        .map(metadata -> ApplicationType.CONTINUATION_APPLICATION.equals(metadata.getApplicationType()))
        .orElse(false);
  }

  @Override
  public DocumentMailMergeFieldResolveResult resolve(DocumentInstanceDto documentInstanceDto) {
    return DocumentMailMergeFieldResolveResult.successNoEsc(
        documentLinkingService.getNextTermPhaseStartDateFromDto(documentInstanceDto));
  }
}
