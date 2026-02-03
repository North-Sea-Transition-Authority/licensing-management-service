package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationWpaRequirementValidatorTest {

  private LicenceContinuationWpaRequirementValidator validator;

  @BeforeEach
  void setUp() {
    validator = new LicenceContinuationWpaRequirementValidator();
  }

  @Test
  void isValid_FromIsValid() {
    var formCompleted = new LicenceContinuationWpaRequirementForm();
    formCompleted.setWorkProgrammeActivitiesCompletionStatus(true);
    Errors errors1 = new BeanPropertyBindingResult(formCompleted, "form");

    assertThat(validator.isValid(formCompleted, errors1)).isTrue();

    var formNotCompleted = new LicenceContinuationWpaRequirementForm();
    formNotCompleted.setWorkProgrammeActivitiesCompletionStatus(false);
    formNotCompleted.setActionsToCompleteWorkProgrammeActivities("Drilling required");
    Errors errors2 = new BeanPropertyBindingResult(formNotCompleted, "form");

    assertThat(validator.isValid(formNotCompleted, errors2)).isTrue();
  }

  @Test
  void isValid_FormIsInvalid() {
    var formMissingStatus = new LicenceContinuationWpaRequirementForm();
    Errors errors1 = new BeanPropertyBindingResult(formMissingStatus, "form");

    assertThat(validator.isValid(formMissingStatus, errors1)).isFalse();
    assertThat(errors1.getFieldError("workProgrammeActivitiesCompletionStatus")).isNotNull();

    var formMissingActions = new LicenceContinuationWpaRequirementForm();
    formMissingActions.setWorkProgrammeActivitiesCompletionStatus(false);
    formMissingActions.setActionsToCompleteWorkProgrammeActivities(null);
    Errors errors2 = new BeanPropertyBindingResult(formMissingActions, "form");

    assertThat(validator.isValid(formMissingActions, errors2)).isFalse();
    assertThat(errors2.getFieldError("actionsToCompleteWorkProgrammeActivities")).isNotNull();
  }
}