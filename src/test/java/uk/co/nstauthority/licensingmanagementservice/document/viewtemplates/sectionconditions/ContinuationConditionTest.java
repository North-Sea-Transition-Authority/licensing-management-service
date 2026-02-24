package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.sectionconditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;

// TODO: replace with a proper test once real condition logic is implemented
@ExtendWith(MockitoExtension.class)
class ContinuationConditionTest {

  private final ContinuationCondition condition = new ContinuationCondition();

  @Mock
  private DocumentTemplateDto templateDto;

  @Mock
  private DocumentInstanceDto instanceDto;

  @Test
  void getMnemonic_shouldReturnConstant() {
    assertEquals(ContinuationCondition.MNEMONIC, condition.getMnemonic());
  }

  @Test
  void getTitle_shouldReturnConstant() {
    assertEquals(ContinuationCondition.TITLE, condition.getTitle());
  }

  @Test
  void isApplicable_shouldAlwaysReturnTrue() {
    assertTrue(condition.isApplicable(templateDto));
  }

  @Test
  void evaluate_shouldAlwaysReturnTrue() {
    assertTrue(condition.evaluate(instanceDto));
  }
}