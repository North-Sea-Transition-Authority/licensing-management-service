package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

public interface PositionValidationRule {

  String getMessage();

  default String getMessage(Object... args) {
    return getMessage().formatted(args);
  }
}
