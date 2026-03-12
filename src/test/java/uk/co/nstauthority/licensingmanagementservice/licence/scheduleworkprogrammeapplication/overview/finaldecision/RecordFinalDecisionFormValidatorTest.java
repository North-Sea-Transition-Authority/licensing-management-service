package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;
import uk.co.nstauthority.licensingmanagementservice.file.FileUploadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class RecordFinalDecisionFormValidatorTest {

  @InjectMocks
  private RecordFinalDecisionFormValidator validator;

  private RecordFinalDecisionForm buildValidForm() {
    var form = new RecordFinalDecisionForm();
    form.getDecisionDate().setDate(LocalDate.of(2024, 3, 15));
    form.setFinalDecisionSupportPapers(List.of(
        FileUploadTestUtil.getUploadedFileFormWithDescription("decision.pdf", "Final decision paper")));
    return form;
  }

  @Test
  void isValid_whenValidDateAndOneFile_returnsTrue() {
    var form = buildValidForm();
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenDateEmpty_returnsFalse() {
    var form = buildValidForm();
    form.setDecisionDate(new ThreeFieldDateInput("decisionDate", "decision date"));
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasErrors()).isTrue();
  }

  @Test
  void isValid_whenNoFiles_returnsFalse() {
    var form = buildValidForm();
    form.setFinalDecisionSupportPapers(List.of());
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("finalDecisionSupportPapers")).isTrue();
  }
}
