package uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class ExternalContributorFormValidatorTest {

  private ExternalContributorFormValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ExternalContributorFormValidator();
  }

  @Test
  void isValid_whenAnswerSelected_isValid() {
    var formYes = new ExternalContributorForm();
    formYes.setAddExternalContributors(true);
    Errors errorsYes = new BeanPropertyBindingResult(formYes, "form");

    assertThat(validator.isValid(errorsYes)).isTrue();

    var formNo = new ExternalContributorForm();
    formNo.setAddExternalContributors(false);
    Errors errorsNo = new BeanPropertyBindingResult(formNo, "form");

    assertThat(validator.isValid(errorsNo)).isTrue();
  }

  @Test
  void isValid_whenNoAnswerSelected_isInvalid() {
    var form = new ExternalContributorForm();
    Errors errors = new BeanPropertyBindingResult(form, "form");

    assertThat(validator.isValid(errors)).isFalse();
    assertThat(errors.getFieldError("addExternalContributors")).isNotNull();
  }
}
