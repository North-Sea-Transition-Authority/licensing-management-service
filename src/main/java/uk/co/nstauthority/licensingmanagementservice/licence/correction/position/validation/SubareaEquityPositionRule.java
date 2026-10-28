package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

public enum SubareaEquityPositionRule implements PositionValidationRule {

  SINGLE_CHANGE_PER_SUBAREA_PER_TRANSACTION(
      "A licence on a transaction can only have one beneficial interest change per subarea"),
  EQUITY_HOLDER_OUT_OF_RANGE("Each subarea holder must have between 0% and 100% equity"),
  BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED(
      "The sum of all beneficial interests in a subarea at a given licence position must equal 100%"),
  PRODUCTION_LICENCE_ONLY(
      "A subarea beneficial interest change can only be applied on a production licence");

  private final String message;

  SubareaEquityPositionRule(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}