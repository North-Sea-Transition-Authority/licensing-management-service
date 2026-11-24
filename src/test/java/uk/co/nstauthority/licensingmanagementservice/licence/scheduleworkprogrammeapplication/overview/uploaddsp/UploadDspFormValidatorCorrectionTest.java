package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.file.FileUploadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

class UploadDspFormValidatorCorrectionTest {

  private final UploadDspFormValidator validator = new UploadDspFormValidator(Clock.systemUTC());

  private UploadDspForm buildValidForm() {
    var form = new UploadDspForm();
    form.setDspAction(UploadDspAction.CORRECT);
    form.setFinalDecisionSupportPapers(List.of(
        FileUploadTestUtil.getUploadedFileFormWithDescription("corrected.pdf", "Corrected paper")));
    return form;
  }

  @Test
  void isValidUpdate_whenActionAndFile_returnsTrue() {
    var form = buildValidForm();
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValidUpdate(form, bindingResult)).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValidUpdate_whenNoAction_returnsFalse() {
    var form = buildValidForm();
    form.setDspAction(null);
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValidUpdate(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("dspAction")).isTrue();
  }

  @Test
  void isValidUpdate_whenNoFiles_returnsFalse() {
    var form = buildValidForm();
    form.setFinalDecisionSupportPapers(List.of());
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValidUpdate(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("finalDecisionSupportPapers")).isTrue();
  }

  @Test
  void isValidUpdate_whenAddendumActionAndFile_returnsTrue() {
    var form = new UploadDspForm();
    form.setDspAction(UploadDspAction.ADD_ADDENDUM);
    form.setDspAddendums(List.of(
        FileUploadTestUtil.getUploadedFileFormWithDescription("addendum.pdf", "Addendum")));
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValidUpdate(form, bindingResult)).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValidUpdate_whenAddendumActionAndNoAddendumFiles_returnsFalse() {
    var form = buildValidForm();
    form.setDspAction(UploadDspAction.ADD_ADDENDUM);
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValidUpdate(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("dspAddendums")).isTrue();
    assertThat(bindingResult.hasFieldErrors("finalDecisionSupportPapers")).isFalse();
  }
}
