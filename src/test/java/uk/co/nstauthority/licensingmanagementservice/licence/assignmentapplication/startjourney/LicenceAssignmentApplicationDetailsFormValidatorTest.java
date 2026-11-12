package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

class LicenceAssignmentApplicationDetailsFormValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2026, Month.NOVEMBER, 6);

  private static final Set<Integer> AVAILABLE_ORGANISATION_UNIT_IDS = Set.of(1, 2);

  private final LicenceAssignmentApplicationDetailsFormValidator validator =
      new LicenceAssignmentApplicationDetailsFormValidator(
          Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
      );

  @Test
  void isValid_whenFormValid_thenNoErrors() {
    var form = buildValidForm();
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    var isValid = validator.isValid(form, bindingResult, AVAILABLE_ORGANISATION_UNIT_IDS);

    assertThat(isValid).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenFormEmpty_thenRequiredErrors() {
    var form = new LicenceAssignmentApplicationDetailsForm();
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    var isValid = validator.isValid(form, bindingResult, AVAILABLE_ORGANISATION_UNIT_IDS);

    assertThat(isValid).isFalse();
    assertThat(ValidatorTestingUtil.extractErrorMessages(bindingResult)).isEqualTo(Map.of(
        "organisationUnitId", Set.of("Select the organisation this application is on behalf of"),
        "applicationReference.inputValue", Set.of("Enter an application reference"),
        "plannedDate.dayInput.inputValue", Set.of("Enter the planned completion date"),
        "plannedDate.monthInput.inputValue", Set.of(""),
        "plannedDate.yearInput.inputValue", Set.of("")
    ));
  }

  @Test
  void isValid_whenOrganisationNotAvailable_thenError() {
    var form = buildValidForm();
    form.setOrganisationUnitId(3);
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    var isValid = validator.isValid(form, bindingResult, AVAILABLE_ORGANISATION_UNIT_IDS);

    assertThat(isValid).isFalse();
    assertThat(ValidatorTestingUtil.extractErrorMessages(bindingResult)).isEqualTo(Map.of(
        "organisationUnitId", Set.of("Select a valid organisation")
    ));
  }

  @Test
  void isValid_whenReferenceTooLong_thenError() {
    var form = buildValidForm();
    form.getApplicationReference().setInputValue("a".repeat(101));
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    var isValid = validator.isValid(form, bindingResult, AVAILABLE_ORGANISATION_UNIT_IDS);

    assertThat(isValid).isFalse();
    assertThat(ValidatorTestingUtil.extractErrorMessages(bindingResult)).isEqualTo(Map.of(
        "applicationReference.inputValue", Set.of("Application reference must be 100 characters or fewer")
    ));
  }

  @Test
  void isValid_whenPlannedDateInPast_thenError() {
    var form = buildValidForm();
    form.getPlannedDate().setDate(TODAY.minusDays(1));
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    var isValid = validator.isValid(form, bindingResult, AVAILABLE_ORGANISATION_UNIT_IDS);

    assertThat(isValid).isFalse();
    assertThat(ValidatorTestingUtil.extractErrorMessages(bindingResult)).isEqualTo(Map.of(
        "plannedDate.dayInput.inputValue", Set.of("Planned completion date must be today or in the future"),
        "plannedDate.monthInput.inputValue", Set.of(""),
        "plannedDate.yearInput.inputValue", Set.of("")
    ));
  }

  private LicenceAssignmentApplicationDetailsForm buildValidForm() {
    var form = new LicenceAssignmentApplicationDetailsForm();
    form.setOrganisationUnitId(1);
    form.getApplicationReference().setInputValue("a".repeat(100));
    form.getPlannedDate().setDate(TODAY);
    return form;
  }
}
