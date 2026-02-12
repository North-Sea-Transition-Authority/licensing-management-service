package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationOtherRequirementValidatorTest {

  private LicenceContinuationOtherRequirementValidator licenceContinuationOtherRequirementValidator;

  @BeforeEach
  void setUp() {
    licenceContinuationOtherRequirementValidator = new LicenceContinuationOtherRequirementValidator();
  }

  @Test
  void isValid_ReturnsTrue_WhenFormIsCorrect() {
    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();
    licenceContinuationOtherRequirementForm.setFinancialCapacityEvidenceSubmissionStatus(true);
    licenceContinuationOtherRequirementForm.setDevelopmentConsentGrantStatus(true);
    licenceContinuationOtherRequirementForm.setRelinquishmentRequirementStatus(true);
    Errors errors1 = new BeanPropertyBindingResult(licenceContinuationOtherRequirementForm, "form");

    assertThat(licenceContinuationOtherRequirementValidator.isValid(licenceContinuationOtherRequirementForm, errors1)).isTrue();

    var licenceContinuationOtherRequirementForm2 = new LicenceContinuationOtherRequirementForm();
    licenceContinuationOtherRequirementForm2.setFinancialCapacityEvidenceSubmissionStatus(false);
    licenceContinuationOtherRequirementForm2.setDevelopmentConsentGrantStatus(false);
    licenceContinuationOtherRequirementForm2.setRelinquishmentRequirementStatus(false);
    licenceContinuationOtherRequirementForm2.setActionsToProvideFinancialEvidence("Will submit next week");
    licenceContinuationOtherRequirementForm2.setActionsToApproveDevelopmentConsent("Will submit next month");
    licenceContinuationOtherRequirementForm2.setActionsToRelinquishRequiredLicenceArea("Relinquishing 50% area");
    Errors errors2 = new BeanPropertyBindingResult(licenceContinuationOtherRequirementForm2, "form");

    assertThat(licenceContinuationOtherRequirementValidator.isValid(licenceContinuationOtherRequirementForm2, errors2)).isTrue();
  }

  @Test
  void isValid_ReturnsFalse_WhenFormIsInvalid() {
    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();
    Errors errors1 = new BeanPropertyBindingResult(licenceContinuationOtherRequirementForm, "form");

    assertThat(licenceContinuationOtherRequirementValidator.isValid(licenceContinuationOtherRequirementForm, errors1)).isFalse();
    assertThat(errors1.getFieldError("financialCapacityEvidenceSubmissionStatus")).isNotNull();
    assertThat(errors1.getFieldError("developmentConsentGrantStatus")).isNotNull();
    assertThat(errors1.getFieldError("relinquishmentRequirementStatus")).isNotNull();

    var licenceContinuationOtherRequirementForm2 = new LicenceContinuationOtherRequirementForm();
    licenceContinuationOtherRequirementForm2.setFinancialCapacityEvidenceSubmissionStatus(false);
    licenceContinuationOtherRequirementForm2.setActionsToProvideFinancialEvidence(null);
    licenceContinuationOtherRequirementForm2.setDevelopmentConsentGrantStatus(false);
    licenceContinuationOtherRequirementForm2.setActionsToApproveDevelopmentConsent(null);
    licenceContinuationOtherRequirementForm2.setRelinquishmentRequirementStatus(false);
    licenceContinuationOtherRequirementForm2.setActionsToRelinquishRequiredLicenceArea(null);
    Errors errors2 = new BeanPropertyBindingResult(licenceContinuationOtherRequirementForm2, "form");

    assertThat(licenceContinuationOtherRequirementValidator.isValid(licenceContinuationOtherRequirementForm2, errors2)).isFalse();
    assertThat(errors2.getFieldError("actionsToProvideFinancialEvidence")).isNotNull();
    assertThat(errors2.getFieldError("actionsToApproveDevelopmentConsent")).isNotNull();
    assertThat(errors2.getFieldError("actionsToRelinquishRequiredLicenceArea")).isNotNull();
  }
}