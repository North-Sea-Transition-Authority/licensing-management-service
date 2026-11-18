package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderService;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;


@Service
public class CorrectPositionChangeTypeFormValidator {

  private static final String DATE_HAS_PARTIAL_SURRENDER_ERROR_MESSAGE =
      "Enter a date that does not already have a partial surrender for this transaction";

  private final Clock clock;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final CorrectChangeOrderService correctChangeOrderService;

  CorrectPositionChangeTypeFormValidator(
      Clock clock,
      LicencePositionCorrectionService licencePositionCorrectionService,
      CorrectChangeOrderService correctChangeOrderService
  ) {
    this.clock = clock;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.correctChangeOrderService = correctChangeOrderService;
  }

  boolean hasErrors(
      CorrectPositionChangeTypeForm form,
      Errors errors,
      LicenceCorrection correction,
      UUID sourcePositionId,
      UUID changeId
  ) {
    var today = LocalDate.now(clock);
    ThreeFieldDateInputValidator.builder()
        .mustBeBeforeOrEqualTo(today)
        .mustBeBeforeOrEqualToErrorMessage(
            "Execution date must be the same as or before %s".formatted(DateUtil.formatLongDate(today))
        )
        .validate(form.getCorrectPositionDate(), errors);

    if (!errors.hasErrors()) {
      var positionDate = form.getCorrectPositionDate().getAsLocalDate().orElseThrow();
      var matchingPositionId = licencePositionCorrectionService
          .findSameTransactionPositionOnDate(correction, sourcePositionId, positionDate)
          .matchingPositionId();

      if (matchingPositionId != null
          && cannotMoveChange(correction, sourcePositionId, changeId, matchingPositionId)) {
        rejectDateWithPartialSurrender(errors);
      }
    }

    return errors.hasErrors();
  }

  private boolean cannotMoveChange(
      LicenceCorrection correction,
      UUID sourcePositionId,
      UUID changeId,
      UUID targetPositionId
  ) {
    return !correctChangeOrderService.canMoveChangeToPosition(correction, sourcePositionId, changeId, targetPositionId);
  }

  private static void rejectDateWithPartialSurrender(Errors errors) {
    errors.rejectValue(
        "correctPositionDate.dayInput.inputValue",
        "correctPositionDate.hasPartialSurrender",
        DATE_HAS_PARTIAL_SURRENDER_ERROR_MESSAGE
    );
    errors.rejectValue("correctPositionDate.monthInput.inputValue", "correctPositionDate.hasPartialSurrender", "");
    errors.rejectValue("correctPositionDate.yearInput.inputValue", "correctPositionDate.hasPartialSurrender", "");
  }
}