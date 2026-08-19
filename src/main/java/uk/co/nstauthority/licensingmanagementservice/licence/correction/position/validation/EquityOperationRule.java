package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

public enum EquityOperationRule implements PositionValidationRule {

  CARBON_STORAGE_LICENCE_ONLY(
      "A carbon storage beneficial interest change can only be applied on a carbon storage licence"),
  INSUFFICIENT_EQUITY_TO_TRANSFER(
      "The organisation does not hold enough equity to make this transfer");

  private final String message;

  EquityOperationRule(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}