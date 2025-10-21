package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceWorkProgrammeAmendmentSummaryFormValidatorTest {

  @InjectMocks
  private LicenceWorkProgrammeAmendmentSummaryFormValidator validator;

  private LicenceWorkProgrammeAmendmentSummaryForm form;
  private Errors errors;
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    form = new LicenceWorkProgrammeAmendmentSummaryForm();
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setId(UUID.randomUUID());
  }

  @Test
  void isValid_true_optionsFieldSelected() {
    form.setLicenceWorkProgrammeAmendmentSummaryOptions(LicenceWorkProgrammeAmendmentSummaryOptions.YES_LATER);
    errors = new BeanPropertyBindingResult(form, "form");

    boolean result = validator.isValid(errors);

    assertTrue(result);
  }

  @Test
  void isValid_false_optionsFieldNotSelected() {
    form.setLicenceWorkProgrammeAmendmentSummaryOptions(null);
    errors = new BeanPropertyBindingResult(form, "form");

    boolean result = validator.isValid(errors);

    assertFalse(result);
    assertThat(Objects
        .requireNonNull(errors.getFieldError("licenceWorkProgrammeAmendmentSummaryOptions"))
        .getCode()).isEqualTo("licenceWorkProgrammeAmendmentSummaryOptions.required");
  }
}