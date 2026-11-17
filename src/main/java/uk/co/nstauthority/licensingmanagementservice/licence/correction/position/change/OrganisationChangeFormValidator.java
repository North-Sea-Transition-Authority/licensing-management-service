package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;

@Component
public class OrganisationChangeFormValidator {
  private static final String OPERATOR_ID_FIELD = "id.inputValue";
  private static final String VALID_OPERATOR_CODE = "id.invalid";
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public OrganisationChangeFormValidator(OrganisationUnitQueryService organisationUnitQueryService) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public boolean hasErrors(
      OrganisationChangeForm form,
      Errors errors,
      Integer previousOperatorId,
      String operatorType
  ) {
    var article = operatorType.toLowerCase().matches("[aeiou].*") ? "an" : "a";
    StringInputValidator.builder()
        .emptyInputErrorMessage("Select %s %s".formatted(article, operatorType))
        .validate(form.getId(), errors);

    if (errors.hasErrors()) {
      return true;
    }

    var validOperatorMessage = "Select a valid %s".formatted(operatorType);
    int newOperatorId;
    try {
      newOperatorId = Integer.parseInt(form.getId().getInputValue());
    } catch (NumberFormatException e) {
      errors.rejectValue(
          OPERATOR_ID_FIELD,
          VALID_OPERATOR_CODE,
          validOperatorMessage
      );
      return true;
    }

    if (organisationUnitQueryService.getOrganisationUnit(newOperatorId).isEmpty()) {
      errors.rejectValue(
          OPERATOR_ID_FIELD,
          VALID_OPERATOR_CODE,
          validOperatorMessage
      );
      return true;
    }


    var lastWord = operatorType.substring(operatorType.trim().lastIndexOf(' ') + 1).trim();

    if (Objects.equals(previousOperatorId, newOperatorId)) {
      errors.rejectValue(
          OPERATOR_ID_FIELD,
          "operatorId.sameAsPrevious",
          "The new %s must be different to the previous %s".formatted(operatorType, lastWord)
      );
    }

    return errors.hasErrors();
  }
}
