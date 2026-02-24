package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.sectionconditions;

import static uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.sectionconditions.SectionConditionDisplayOrders.CONTINUATION_EXAMPLE_DISPLAY_ORDER;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionCondition;

//TODO: remove when we implement a real world condition
@Order(CONTINUATION_EXAMPLE_DISPLAY_ORDER)
@Component
public class ContinuationCondition implements DocumentTemplateSectionCondition {

  public static final String MNEMONIC = "CONTINUATION_EXAMPLE";
  static final String TITLE = "Continuation example";

  @Override
  public String getMnemonic() {
    return MNEMONIC;
  }

  @Override
  public String getTitle() {
    return TITLE;
  }

  @Override
  public boolean isApplicable(DocumentTemplateDto documentTemplateDto) {
    return true;
  }

  @Override
  public boolean evaluate(DocumentInstanceDto documentInstanceDto) {
    return true;
  }
}
