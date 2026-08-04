package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

class RecordDecisionFormValidatorTest {

  private final RecordDecisionFormValidator recordDecisionFormValidator = new RecordDecisionFormValidator();

  private RecordDecisionForm form;
  private Errors errors;

  @BeforeEach
  void setUp() {
    form = new RecordDecisionForm();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void isValid_whenBothAnswered_assertValidWithNoErrors() {
    form.setExtensionDecision(RecordOfDecisionResponse.GRANTED);
    form.setWorkProgrammeDecision(RecordOfDecisionResponse.NOT_REQUESTED);

    assertThat(recordDecisionFormValidator.isValid(form, errors)).isTrue();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenExtensionDecisionMissing_assertRejected() {
    form.setWorkProgrammeDecision(RecordOfDecisionResponse.GRANTED);

    assertThat(recordDecisionFormValidator.isValid(form, errors)).isFalse();
    assertThat(errors.hasFieldErrors("extensionDecision")).isTrue();
  }

  @Test
  void isValid_whenWorkProgrammeDecisionMissing_assertRejected() {
    form.setExtensionDecision(RecordOfDecisionResponse.GRANTED);

    assertThat(recordDecisionFormValidator.isValid(form, errors)).isFalse();
    assertThat(errors.hasFieldErrors("workProgrammeDecision")).isTrue();
  }

  @Test
  void isValid_whenBothMissing_assertBothRejected() {
    assertThat(recordDecisionFormValidator.isValid(form, errors)).isFalse();
    assertThat(errors.hasFieldErrors("extensionDecision")).isTrue();
    assertThat(errors.hasFieldErrors("workProgrammeDecision")).isTrue();
  }
}
