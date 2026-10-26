package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

public enum PartialSurrenderOperationRule implements PositionValidationRule {

  BLOCKS_REQUIRED(
      "Select the licence blocks being surrendered"
  ),
  BLOCKS_ALREADY_CHANGED(
      "Select licence blocks that do not already have a change"
  ),
  SURRENDER_TYPE_REQUIRED(
      "Select the type of surrender for this block"
  ),
  NO_PARTIAL_SURRENDER(
      "Select partial surrender for at least one block"
  ),
  AREAS_REQUIRED(
      "Select the areas being surrendered"
  ),
  CANNOT_SURRENDER_ALL_AREAS(
      "You cannot surrender all the areas in a partial surrender"
  ),
  CANNOT_SURRENDER_WHOLE_BLOCK(
      "Select areas that do not cover the whole block"
  ),
  KEPT_AREA_OVERLAPS_BLOCK(
      "Block after surrender must not overlap licence block %s"
  ),
  AREAS_CHANGED(
      "Select the areas being surrendered again as the block's retained area has changed"
  );

  private final String message;

  PartialSurrenderOperationRule(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
