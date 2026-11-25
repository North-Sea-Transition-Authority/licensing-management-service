package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;

@Service
public class AddLicencePositionStagedPayloadFormValidator {

  private final Clock clock;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  AddLicencePositionStagedPayloadFormValidator(
      Clock clock,
      LicencePositionStagedPayloadService licencePositionStagedPayloadService
  ) {
    this.clock = clock;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
  }

  boolean hasErrors(AddLicencePositionStagedPayloadForm form, LicenceCorrection correction, Errors errors) {
    StringInputValidator.builder().validate(form.getCorrectionReference(), errors);

    if (!errors.hasFieldErrors("correctionReference.inputValue")
        && licencePositionStagedPayloadService.isCorrectionReferenceInUse(
        correction, form.getCorrectionReference().getInputValue())) {
      errors.rejectValue(
          "correctionReference.inputValue",
          "correctionReference.duplicate",
          "Enter a correction reference that has not already been used"
      );
    }

    ThreeFieldDateInputValidator.builder().mustBeBeforeOrEqualTo(LocalDate.now(clock)).validate(form.getPositionDate(), errors);

    return errors.hasErrors();
  }
}