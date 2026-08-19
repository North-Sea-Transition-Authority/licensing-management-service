package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

public enum EquityPositionRule implements PositionValidationRule {

  SINGLE_CHANGE_PER_TRANSACTION(
      "A licence on a transaction can only have one carbon storage beneficial interest change"),
  EQUITY_HOLDER_OUT_OF_RANGE(
      "Each holder must have between 0% and 100% equity"),
  BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED(
      "The sum of all beneficial interests at a given licence position must equal 100%");

  private final String message;

  EquityPositionRule(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}