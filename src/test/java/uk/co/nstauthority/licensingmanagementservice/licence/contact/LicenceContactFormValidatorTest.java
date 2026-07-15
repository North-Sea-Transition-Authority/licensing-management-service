package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceContactFormValidatorTest {

  @InjectMocks
  private LicenceContactFormValidator validator;

  @Test
  void isValid_whenBlank_rejectsAsRequired() {
    var form = new LicenceContactForm();
    form.setContactEmail("  ");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult, List.of());

    ValidatorTestingUtil.assertErrorExists(
        bindingResult, "contactEmail", "contactEmail.required", "Enter a contact email address");
  }

  @Test
  void isValid_whenNull_rejectsAsRequired() {
    var form = new LicenceContactForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult, List.of());

    ValidatorTestingUtil.assertErrorExists(
        bindingResult, "contactEmail", "contactEmail.required", "Enter a contact email address");
  }

  @Test
  void isValid_whenNotAnEmail_rejectsAsInvalid() {
    var form = new LicenceContactForm();
    form.setContactEmail("not-an-email");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult, List.of());

    ValidatorTestingUtil.assertErrorExists(
        bindingResult, "contactEmail", "contactEmail.invalid", "Enter a valid email address");
  }

  @Test
  void isValid_whenValidEmail_hasNoErrors() {
    var form = new LicenceContactForm();
    form.setContactEmail("licensing@example.com");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult, List.of());

    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenSelectedLicenceDeparted_rejectsAsDeparted() {
    var form = new LicenceContactForm();
    form.setContactEmail("licensing@example.com");
    form.setBulkUpdateLicenceIds(List.of(2, 3));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult, List.of(1, 2));

    ValidatorTestingUtil.assertErrorExists(
        bindingResult, "bulkUpdateLicenceIds", "bulkUpdateLicenceIds.departed",
        "One or more selected licences are no longer held by this licensee");
  }

  @Test
  void isValid_whenSelectionStillValid_hasNoErrors() {
    var form = new LicenceContactForm();
    form.setContactEmail("licensing@example.com");
    form.setBulkUpdateLicenceIds(List.of(2));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult, List.of(1, 2));

    assertThat(bindingResult.hasErrors()).isFalse();
  }
}
