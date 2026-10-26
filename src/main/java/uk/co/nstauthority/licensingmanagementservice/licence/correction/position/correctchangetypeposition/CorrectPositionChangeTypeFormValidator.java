package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderService;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;


@Service
public class CorrectPositionChangeTypeFormValidator {

  private static final String SELECT_MOVE_ERROR_MESSAGE = "Select where to move the change to";
  private static final String POSITION_HAS_PARTIAL_SURRENDER_ERROR_MESSAGE =
      "Select a position that does not already have a partial surrender";
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
      Set<String> allowedMoves,
      LicenceCorrection correction,
      UUID sourcePositionId,
      UUID changeId
  ) {
    StringInputValidator.builder()
        .emptyInputErrorMessage(SELECT_MOVE_ERROR_MESSAGE)
        .validate(form.getChangeTypePositionMove(), errors);

    var value = form.getChangeTypePositionMove().getInputValue();

    if (CorrectPositionChangeTypeForm.OTHER_DATE_OPTION.equals(value)) {
      var today = LocalDate.now(clock);
      ThreeFieldDateInputValidator.builder()
          .mustBeBeforeOrEqualTo(today)
          .mustBeBeforeOrEqualToErrorMessage(
              "Position date must be the same as or before %s".formatted(DateUtil.formatLongDate(today))
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
    } else if (!StringUtils.isBlank(value) && !allowedMoves.contains(value)) {
      errors.rejectValue(
          "changeTypePositionMove.inputValue",
          "changeTypePositionMove.invalid",
          SELECT_MOVE_ERROR_MESSAGE
      );
    } else if (!StringUtils.isBlank(value)
        && cannotMoveChange(correction, sourcePositionId, changeId, UUID.fromString(value))) {
      errors.rejectValue(
          "changeTypePositionMove.inputValue",
          "changeTypePositionMove.hasPartialSurrender",
          POSITION_HAS_PARTIAL_SURRENDER_ERROR_MESSAGE
      );
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