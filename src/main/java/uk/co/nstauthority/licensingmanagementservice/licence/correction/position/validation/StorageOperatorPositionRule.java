package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

public enum StorageOperatorPositionRule implements PositionValidationRule {

  ONLY_ONE_STORAGE_OPERATOR_CHANGE(
      "A licence position can only have one storage operator change");

  private final String message;

  StorageOperatorPositionRule(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
