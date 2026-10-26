package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;

@Component
public class StorageOperatorChangeFormValidator {

  private static final String OPERATOR_ID_FIELD = "operatorId.inputValue";
  private static final String VALID_OPERATOR_MESSAGE = "Select a valid storage operator";
  private static final String VALID_OPERATOR_CODE = "operatorId.invalid";
  private final OrganisationUnitQueryService organisationUnitQueryService;

  StorageOperatorChangeFormValidator(OrganisationUnitQueryService organisationUnitQueryService) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public boolean hasErrors(
      StorageOperatorChangeForm form,
      Errors errors,
      Integer previousOperatorId
  ) {
    StringInputValidator.builder()
        .emptyInputErrorMessage("Select a storage operator")
        .validate(form.getOperatorId(), errors);

    if (errors.hasErrors()) {
      return true;
    }

    int newOperatorId;
    try {
      newOperatorId = Integer.parseInt(form.getOperatorId().getInputValue());
    } catch (NumberFormatException e) {
      errors.rejectValue(
          OPERATOR_ID_FIELD,
          VALID_OPERATOR_CODE,
          VALID_OPERATOR_MESSAGE
      );
      return true;
    }

    if (organisationUnitQueryService.getOrganisationUnit(newOperatorId).isEmpty()) {
      errors.rejectValue(
          OPERATOR_ID_FIELD,
          VALID_OPERATOR_CODE,
          VALID_OPERATOR_MESSAGE
      );
      return true;
    }

    if (Objects.equals(previousOperatorId, newOperatorId)) {
      errors.rejectValue(
          OPERATOR_ID_FIELD,
          "operatorId.sameAsPrevious",
          "The new storage operator must be different to the previous operator"
      );
    }

    return errors.hasErrors();
  }
}
