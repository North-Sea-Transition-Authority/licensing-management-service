package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeField;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;

@Order(DocumentMailMergeFieldDisplayOrders.COMPANY_REGISTERED_ADDRESS)
@Component
public class CompanyRegisteredAddressMailMergeField implements DocumentMailMergeField {

  static final String MNEMONIC = "COMPANY_REGISTERED_ADDRESS";
  static final String DESCRIPTION = "The registered address of the company";

  private final DocumentLinkingService documentLinkingService;

  @Autowired
  public CompanyRegisteredAddressMailMergeField(DocumentLinkingService documentLinkingService) {
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
    var applicationCompanyAddress = documentLinkingService.getApplicationCompanyAddressFromDto(documentInstanceDto);

    if (applicationCompanyAddress == null) {
      return DocumentMailMergeFieldResolveResult.success("");
    }

    var registeredAddress = applicationCompanyAddress.getFormattedAddress()
        .replace("\r\n", "<br/>")
        .replace("\n", "<br/>");

    return DocumentMailMergeFieldResolveResult.successNoEsc(registeredAddress);
  }
}
