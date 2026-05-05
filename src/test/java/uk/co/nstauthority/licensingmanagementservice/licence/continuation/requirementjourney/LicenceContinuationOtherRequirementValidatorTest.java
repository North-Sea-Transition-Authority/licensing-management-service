package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationOtherRequirementValidatorTest {

  private LicenceContinuationOtherRequirementValidator licenceContinuationOtherRequirementValidator;

  @Mock
  private OtherRequirementsVisibility otherRequirementsVisibility;

  @BeforeEach
  void setUp() {
    licenceContinuationOtherRequirementValidator = new LicenceContinuationOtherRequirementValidator();
  }

  @Test
  void isValid_ReturnsTrue_WhenFormIsCorrectAndAllVisible() {
    when(otherRequirementsVisibility.showFinancialCapacity()).thenReturn(true);
    when(otherRequirementsVisibility.showDevelopmentConsent()).thenReturn(true);
    when(otherRequirementsVisibility.showRelinquishment()).thenReturn(true);

    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();
    licenceContinuationOtherRequirementForm.setFinancialCapacityEvidenceSubmissionStatus(true);
    licenceContinuationOtherRequirementForm.setDevelopmentConsentGrantStatus(true);
    licenceContinuationOtherRequirementForm.setRelinquishmentRequirementStatus(true);
    Errors errors1 = new BeanPropertyBindingResult(licenceContinuationOtherRequirementForm, "form");

    assertThat(licenceContinuationOtherRequirementValidator.isValid(licenceContinuationOtherRequirementForm, errors1, otherRequirementsVisibility)).isTrue();

    var licenceContinuationOtherRequirementForm2 = new LicenceContinuationOtherRequirementForm();
    licenceContinuationOtherRequirementForm2.setFinancialCapacityEvidenceSubmissionStatus(false);
    licenceContinuationOtherRequirementForm2.setDevelopmentConsentGrantStatus(false);
    licenceContinuationOtherRequirementForm2.setRelinquishmentRequirementStatus(false);
    licenceContinuationOtherRequirementForm2.setActionsToProvideFinancialEvidence("Will submit next week");
    licenceContinuationOtherRequirementForm2.setActionsToApproveDevelopmentConsent("Will submit next month");
    licenceContinuationOtherRequirementForm2.setActionsToRelinquishRequiredLicenceArea("Relinquishing 50% area");
    Errors errors2 = new BeanPropertyBindingResult(licenceContinuationOtherRequirementForm2, "form");

    assertThat(licenceContinuationOtherRequirementValidator.isValid(licenceContinuationOtherRequirementForm2, errors2,
                                                                    otherRequirementsVisibility
    )).isTrue();
  }

  @Test
  void isValid_ReturnsFalse_WhenFormIsInvalidAndAllVisible() {
    when(otherRequirementsVisibility.showFinancialCapacity()).thenReturn(true);
    when(otherRequirementsVisibility.showDevelopmentConsent()).thenReturn(true);
    when(otherRequirementsVisibility.showRelinquishment()).thenReturn(true);

    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();
    Errors errors1 = new BeanPropertyBindingResult(licenceContinuationOtherRequirementForm, "form");

    assertThat(licenceContinuationOtherRequirementValidator.isValid(licenceContinuationOtherRequirementForm, errors1,
                                                                    otherRequirementsVisibility
    )).isFalse();
    assertThat(errors1.getFieldError("financialCapacityEvidenceSubmissionStatus")).isNotNull();
    assertThat(errors1.getFieldError("developmentConsentGrantStatus")).isNotNull();
    assertThat(errors1.getFieldError("relinquishmentRequirementStatus")).isNotNull();

    var licenceContinuationOtherRequirementForm2 = getLicenceContinuationOtherRequirementForm();
    Errors errors2 = new BeanPropertyBindingResult(licenceContinuationOtherRequirementForm2, "form");

    assertThat(licenceContinuationOtherRequirementValidator.isValid(licenceContinuationOtherRequirementForm2, errors2,
                                                                    otherRequirementsVisibility
    )).isFalse();
    assertThat(errors2.getFieldError("actionsToProvideFinancialEvidence")).isNotNull();
    assertThat(errors2.getFieldError("actionsToApproveDevelopmentConsent")).isNotNull();
    assertThat(errors2.getFieldError("actionsToRelinquishRequiredLicenceArea")).isNotNull();
  }

  @Test
  void isValid_ReturnsTrue_WhenFormIsInvalidButRequirementsAreNotVisible() {
    when(otherRequirementsVisibility.showFinancialCapacity()).thenReturn(false);
    when(otherRequirementsVisibility.showDevelopmentConsent()).thenReturn(false);
    when(otherRequirementsVisibility.showRelinquishment()).thenReturn(false);

    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();
    Errors errors = new BeanPropertyBindingResult(licenceContinuationOtherRequirementForm, "form");

    assertThat(licenceContinuationOtherRequirementValidator.isValid(licenceContinuationOtherRequirementForm, errors, otherRequirementsVisibility)).isTrue();

    assertThat(errors.hasErrors()).isFalse();
  }

  private LicenceContinuationOtherRequirementForm getLicenceContinuationOtherRequirementForm() {
    var licenceContinuationOtherRequirementForm2 = new LicenceContinuationOtherRequirementForm();
    licenceContinuationOtherRequirementForm2.setFinancialCapacityEvidenceSubmissionStatus(false);
    licenceContinuationOtherRequirementForm2.setActionsToProvideFinancialEvidence(null);
    licenceContinuationOtherRequirementForm2.setDevelopmentConsentGrantStatus(false);
    licenceContinuationOtherRequirementForm2.setActionsToApproveDevelopmentConsent(null);
    licenceContinuationOtherRequirementForm2.setRelinquishmentRequirementStatus(false);
    licenceContinuationOtherRequirementForm2.setActionsToRelinquishRequiredLicenceArea(null);
    return licenceContinuationOtherRequirementForm2;
  }
}