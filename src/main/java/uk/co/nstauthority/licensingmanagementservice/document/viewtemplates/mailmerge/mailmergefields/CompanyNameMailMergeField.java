package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeField;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;


@Order(DocumentMailMergeFieldDisplayOrders.COMPANY_NAME)
@Component
public class CompanyNameMailMergeField implements DocumentMailMergeField {

  static final String MNEMONIC = "COMPANY_NAME";
  static final String DESCRIPTION = "The name of the company";

  private final DocumentLinkingService documentLinkingService;

  @Autowired
  public CompanyNameMailMergeField(
      DocumentLinkingService documentLinkingService
  ) {
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
    var applicationCompanyName = documentLinkingService.getApplicationCompanyNameFromDto(documentInstanceDto);
    return DocumentMailMergeFieldResolveResult.successNoEsc(applicationCompanyName);
  }
}
