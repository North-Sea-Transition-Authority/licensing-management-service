package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Component
public class AddPositionChangeFormValidator {

  private final LicencePositionChangeService licencePositionChangeService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final SubareaChangeService subareaChangeService;

  public AddPositionChangeFormValidator(
      LicencePositionChangeService licencePositionChangeService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      SubareaChangeService subareaChangeService) {
    this.licencePositionChangeService = licencePositionChangeService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.subareaChangeService = subareaChangeService;
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

    if (selected == null || !selected.isAvailableFor(correction.getLicence().getType())) {
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

  private boolean changeAlreadyExists(AddPositionChangeType selected, LicencePositionCorrection positionCorrection) {
    var livePositionId = positionCorrection.getTargetLicencePosition() != null
        ? positionCorrection.getTargetLicencePosition().getId()
        : null;
    return switch (selected) {
      case ADMINISTRATOR ->
          licencePositionChangeService.changeExists(livePositionId, AdministratorOperation.class);
      case SET_EQUITY -> licencePositionChangeService.changeExists(livePositionId, SetEquityOperation.class);
      case TRANSFER_EQUITY -> licencePositionChangeService.changeExists(livePositionId, TransferEquityOperation.class);
      case PARTIAL_SURRENDER ->
          licencePositionChangeService.changeExists(livePositionId, PartialSurrenderOperation.class)
              || partialSurrenderCorrectionService.hasStagedPartialSurrender(positionCorrection);
      case SUBAREA -> licencePositionChangeService.changeExists(livePositionId, SubareaOperation.class)
              || subareaChangeService.hasStagedSubareaChange(positionCorrection);
    };
  }
}