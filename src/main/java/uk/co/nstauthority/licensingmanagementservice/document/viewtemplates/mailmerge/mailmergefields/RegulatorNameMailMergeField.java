package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeField;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties;

@Order(DocumentMailMergeFieldDisplayOrders.REGULATOR_NAME)
@Component
public class RegulatorNameMailMergeField implements DocumentMailMergeField {

  static final String MNEMONIC = "REGULATOR_NAME";
  static final String DESCRIPTION = "The name of the regulator";

  private final CustomerConfigurationProperties customerConfigurationProperties;

  @Autowired
  public RegulatorNameMailMergeField(CustomerConfigurationProperties customerConfigurationProperties) {
    this.customerConfigurationProperties = customerConfigurationProperties;
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
    return DocumentMailMergeFieldResolveResult.successNoEsc(customerConfigurationProperties.name());
  }
}
