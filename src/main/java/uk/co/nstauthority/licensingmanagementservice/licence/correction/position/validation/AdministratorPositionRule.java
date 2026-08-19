package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

public enum AdministratorPositionRule implements PositionValidationRule {

  FIRST_POSITION_MUST_HAVE_ADMINISTRATOR(
      "The first licence position must have an administrator change"),
  ONLY_ONE_ADMINISTRATOR_CHANGE(
      "A licence position can only have one administrator change");

  private final String message;

  AdministratorPositionRule(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}