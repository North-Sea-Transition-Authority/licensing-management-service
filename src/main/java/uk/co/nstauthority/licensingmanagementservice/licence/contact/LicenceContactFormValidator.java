package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import java.util.Collection;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

@Service
public class LicenceContactFormValidator {

  private static final String EMAIL_FIELD = "contactEmail";
  private static final String BULK_UPDATE_FIELD = "bulkUpdateLicenceIds";

  public void isValid(LicenceContactForm form, BindingResult bindingResult, Collection<Integer> validLicenceIds) {
    var email = form.getContactEmail();

    if (!StringUtils.hasText(email)) {
      bindingResult.rejectValue(EMAIL_FIELD, "%s.required".formatted(EMAIL_FIELD), "Enter a contact email address");
    } else if (!EmailValidator.getInstance().isValid(email)) {
      bindingResult.rejectValue(EMAIL_FIELD, "%s.invalid".formatted(EMAIL_FIELD),
          "Enter a valid email address");
    }

    var hasDepartedSelection = form.getBulkUpdateLicenceIds().stream()
        .anyMatch(licenceId -> !validLicenceIds.contains(licenceId));

    if (hasDepartedSelection) {
      bindingResult.rejectValue(BULK_UPDATE_FIELD, "%s.departed".formatted(BULK_UPDATE_FIELD),
          "One or more selected licences are no longer held by this licensee");
    }
  }
}
