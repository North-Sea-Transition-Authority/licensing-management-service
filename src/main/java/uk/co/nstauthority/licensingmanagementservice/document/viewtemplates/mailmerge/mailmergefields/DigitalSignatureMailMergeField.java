package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeField;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;

@Order(DocumentMailMergeFieldDisplayOrders.DIGITAL_SIGNATURE)
@Component
public class DigitalSignatureMailMergeField implements DocumentMailMergeField {

  public static final String SIGNATURE_PLACEHOLDER_TEXT = "((DIGITAL_SIGNATURE))";
  public static final String MNEMONIC = "DIGITAL_SIGNATURE";
  public static final String DESCRIPTION = "The digital signature that will be applied to the document and visible at this " +
      "location. Letters must have exactly one digital signature.";

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
    return DocumentMailMergeFieldResolveResult.success(SIGNATURE_PLACEHOLDER_TEXT);
  }
}