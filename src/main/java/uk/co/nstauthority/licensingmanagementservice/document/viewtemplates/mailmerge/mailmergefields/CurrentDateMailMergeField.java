package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeField;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.formlibrary.validator.date.DateUtils;

@Order(DocumentMailMergeFieldDisplayOrders.CURRENT_DATE)
@Component
public class CurrentDateMailMergeField implements DocumentMailMergeField {

  static final String MNEMONIC = "CURRENT_DATE";
  static final String DESCRIPTION = "Today's date";

  private final Clock clock;

  @Autowired
  public CurrentDateMailMergeField(Clock clock) {
    this.clock = clock;
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
    return DocumentMailMergeFieldResolveResult.success(DateUtils.format(LocalDate.now(clock)));
  }
}
