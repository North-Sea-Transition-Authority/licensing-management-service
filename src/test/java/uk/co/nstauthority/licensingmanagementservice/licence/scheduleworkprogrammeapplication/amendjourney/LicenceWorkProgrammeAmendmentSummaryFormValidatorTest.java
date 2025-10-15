package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

class LicenceWorkProgrammeAmendmentSummaryFormValidatorTest {

  private LicenceWorkProgrammeAmendmentSummaryFormValidator validator;
  private LicenceWorkProgrammeAmendmentSummaryForm form;
  private Errors errors;

  @BeforeEach
  void setUp() {
    validator = new LicenceWorkProgrammeAmendmentSummaryFormValidator();
    form = new LicenceWorkProgrammeAmendmentSummaryForm();
  }

  @Test
  void isValid_returns_true_when_field_is_populated() {
    form.setLicenceWorkProgrammeAmendmentSummaryOptions(LicenceWorkProgrammeAmendmentSummaryOptions.YES_NOW);
    errors = new BeanPropertyBindingResult(form, "form");

    boolean result = validator.isValid(errors);

    assertThat(result).isTrue();
  }

  @Test
  void isValid_returns_false_when_field_is_empty() {
    errors = new BeanPropertyBindingResult(form, "form");

    boolean result = validator.isValid(errors);

    assertThat(result).isFalse();
    assertThat(errors.getFieldError("licenceWorkProgrammeAmendmentSummaryOptions")).isNotNull();
    assertThat(Objects.requireNonNull(errors.getFieldError("licenceWorkProgrammeAmendmentSummaryOptions")).getCode())
        .isEqualTo("licenceWorkProgrammeAmendmentSummaryOptions.required");
  }

  @Test
  void isValid_returns_false_when_field_is_null() {
    form.setLicenceWorkProgrammeAmendmentSummaryOptions(null);
    errors = new BeanPropertyBindingResult(form, "form");

    boolean result = validator.isValid(errors);

    assertThat(result).isFalse();
    assertThat(errors.getFieldError("licenceWorkProgrammeAmendmentSummaryOptions")).isNotNull();
  }
}