package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

@Service
public class SelectApplicationLicenceTypeFormValidator {

  boolean isValid(SelectApplicationLicenceTypeForm form, Errors errors, Collection<LicenceType> availableLicenceTypes) {
    ValidationUtils.rejectIfEmpty(errors, "selectedLicenceType", "selectedLicenceType.required", "Select a licence type");

    var selectedLicenceType = form.getSelectedLicenceType();
    if (selectedLicenceType != null && !availableLicenceTypes.contains(selectedLicenceType)) {
      errors.rejectValue("selectedLicenceType", "selectedLicenceType.notAvailable", "Select a valid licence type");
    }

    return !errors.hasErrors();
  }

}
