package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

@Service
public class LicenceContactFormValidator {

  private static final String FIELD = "contactEmail";

  public void isValid(LicenceContactForm form, BindingResult bindingResult) {
    var email = form.getContactEmail();

    if (!StringUtils.hasText(email)) {
      bindingResult.rejectValue(FIELD, "%s.required".formatted(FIELD), "Enter a contact email address");
    } else if (!EmailValidator.getInstance().isValid(email)) {
      bindingResult.rejectValue(FIELD, "%s.invalid".formatted(FIELD),
          "Enter a valid email address");
    }
  }
}
