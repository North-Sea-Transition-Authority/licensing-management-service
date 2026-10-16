package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

public enum LicenseePositionRule implements PositionValidationRule {

  FIRST_POSITION_MUST_HAVE_LICENSEE(
      "The first licence position must have a licensee change"),
  ONLY_ONE_LICENSEE_CHANGE(
      "A licence position can only have one licensee change"),
  CANNOT_JOIN_A_LICENCE_REPEATEDLY(
      "A licensee cannot join a licence they have already joined"),
  MUST_EXIST_TO_WITHDRAW(
      "A licensee cannot withdraw from a licence they are not on"),
  POSITION_MUST_HAVE_LICENSEES(
      "A licence position must have licensees");

  private final String message;

  LicenseePositionRule(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
