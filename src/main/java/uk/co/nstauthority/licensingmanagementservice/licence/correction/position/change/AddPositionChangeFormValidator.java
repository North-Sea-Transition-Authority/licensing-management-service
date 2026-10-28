package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Component
public class AddPositionChangeFormValidator {

  private static final String CHANGE_TYPE_FIELD = "changeType";
  private static final String SUBAREA_FIELD = "subarea";

  private final LicencePositionChangeService licencePositionChangeService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  public AddPositionChangeFormValidator(
      LicencePositionChangeService licencePositionChangeService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService
  ) {
    this.licencePositionChangeService = licencePositionChangeService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
  }

  public boolean hasErrors(
      AddPositionChangeForm form,
      Errors errors,
      LicenceCorrection correction,
      LicencePositionCorrection positionCorrection,
      Set<String> subareaKeys
  ) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, CHANGE_TYPE_FIELD, "changeType.required",
        "Select the type of change to add");

    if (errors.hasErrors()) {
      return true;
    }

    var selected = parseChangeType(form.getChangeType());

    if (selected == null || !selected.isAvailableFor(correction.getLicence().getType())) {
      errors.rejectValue(CHANGE_TYPE_FIELD, "changeType.invalid", "Select the type of change to add");
    } else if (changeAlreadyExists(selected, positionCorrection)) {
      errors.rejectValue(CHANGE_TYPE_FIELD, "changeType.exists",
          "%s has already been added to this position".formatted(selected.getDisplayName()));
    } else if (AddPositionChangeType.SUBAREA_SET_EQUITY == selected
        && (StringUtils.isBlank(form.getSubarea()) || !subareaKeys.contains(form.getSubarea()))) {
      errors.rejectValue(SUBAREA_FIELD, "subarea.invalid", "Select the subarea to set equity for");
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
      case SUBAREA, SUBAREA_SET_EQUITY -> false;
      case STORAGE_OPERATOR -> licencePositionChangeService.changeExists(livePositionId, StorageOperatorOperation.class);
      case LICENSEE -> licencePositionChangeService.changeExists(livePositionId, LicenseeOperation.class);
    };
  }
}
