package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

public enum SubareaPositionRule implements PositionValidationRule {

  SINGLE_CHANGE_PER_BLOCK_PER_TRANSACTION("A licence on a transaction can only have one subarea change per licence block"),
  BLOCK_MUST_HAVE_SUBAREAS("Each licence block must have at least one active subarea"),
  SUBAREA_NAMES_MUST_BE_UNIQUE("Subarea names must be unique within a licence block"),
  MAP_DISPLAY_NAMES_MUST_BE_UNIQUE("Map display names must be unique within a licence block"),
  NO_CHANGE("Make a change to the subareas");

  private final String message;

  SubareaPositionRule(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
