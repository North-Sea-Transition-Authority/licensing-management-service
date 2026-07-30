package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;

@Component
public class AddPositionChangeFormValidator {

  private final LicenceService licenceService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public AddPositionChangeFormValidator(
      LicenceService licenceService,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.licenceService = licenceService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  public boolean hasErrors(
      AddPositionChangeForm form,
      Errors errors,
      LicenceCorrection correction,
      LicencePositionCorrection positionCorrection
  ) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "changeType", "changeType.required",
        "Select the type of change to add");

    if (errors.hasErrors()) {
      return true;
    }

    var selected = parseChangeType(form.getChangeType());

    if (selected == null || !isAvailable(selected, correction)) {
      errors.rejectValue("changeType", "changeType.invalid", "Select the type of change to add");
    } else if (changeAlreadyExists(selected, positionCorrection)) {
      errors.rejectValue("changeType", "changeType.exists",
          "%s has already been added to this position".formatted(selected.getDisplayName()));
    }

    return errors.hasErrors();
  }

  private AddPositionChangeType parseChangeType(String changeType) {
    try {
      return AddPositionChangeType.valueOf(changeType);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private boolean isAvailable(AddPositionChangeType selected, LicenceCorrection correction) {
    return switch (selected) {
      case ADMINISTRATOR_CHANGE -> true;
      case SET_EQUITY -> licenceService.isCarbonStorageLicence(correction.getLicence());
    };
  }

  private boolean changeAlreadyExists(AddPositionChangeType selected, LicencePositionCorrection positionCorrection) {
    //todo check admin change exists for a live position
    return switch (selected) {
      case ADMINISTRATOR_CHANGE -> false;
      case SET_EQUITY -> licencePositionCorrectionService.setEquityChangeExists(positionCorrection);
    };
  }
}