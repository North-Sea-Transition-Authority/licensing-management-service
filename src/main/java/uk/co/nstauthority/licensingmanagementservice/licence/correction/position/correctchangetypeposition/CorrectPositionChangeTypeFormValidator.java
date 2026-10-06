package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;


@Service
public class CorrectPositionChangeTypeFormValidator {

  private static final String SELECT_MOVE_ERROR_MESSAGE = "Select where to move the change to";

  private final Clock clock;

  CorrectPositionChangeTypeFormValidator(Clock clock) {
    this.clock = clock;
  }

  boolean hasErrors(CorrectPositionChangeTypeForm form, Errors errors, Set<String> allowedMoves) {
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
    } else if (!StringUtils.isBlank(value) && !allowedMoves.contains(value)) {
      errors.rejectValue(
          "changeTypePositionMove.inputValue",
          "changeTypePositionMove.invalid",
          SELECT_MOVE_ERROR_MESSAGE
      );
    }

    return errors.hasErrors();
  }
}