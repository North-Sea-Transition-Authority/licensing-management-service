package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeField;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;

@Order(DocumentMailMergeFieldDisplayOrders.CURRENT_TERM_PHASE_NAME)
@Component
public class CurrentTermPhaseNameMailMergeField implements DocumentMailMergeField {

  static final String MNEMONIC = "CURRENT_TERM_PHASE_NAME";
  static final String DESCRIPTION = "The name of the current term or phase of the licence";

  private final DocumentLinkingService documentLinkingService;

  @Autowired
  public CurrentTermPhaseNameMailMergeField(DocumentLinkingService documentLinkingService) {
    this.documentLinkingService = documentLinkingService;
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
    return true;
  }

  @Override
  public DocumentMailMergeFieldResolveResult resolve(DocumentInstanceDto documentInstanceDto) {
    return DocumentMailMergeFieldResolveResult.successNoEsc(
        documentLinkingService.getCurrentTermPhaseNameFromDto(documentInstanceDto));
  }
}