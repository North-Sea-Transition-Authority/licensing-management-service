package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;

public record PositionValidationError(
    UUID positionId,
    String positionName,
    @Nullable String changeId,
    @Nullable String operationType,
    String message
) {

  public static PositionValidationError forPosition(PositionValidationContext positionValidationContext, String message) {
    var position = positionValidationContext.position();

    return new PositionValidationError(position.id(), position.positionName(), null, null, message);
  }

  public static PositionValidationError forOperation(
      PositionValidationContext positionValidationContext,
      String operationType,
      String message
  ) {
    var position = positionValidationContext.position();

    return new PositionValidationError(position.id(), position.positionName(), null, operationType, message);
  }

  public PositionValidationError withChangeId(String changeId) {
    return new PositionValidationError(positionId, positionName, changeId, operationType, message);
  }

  public static List<ErrorSummaryItem> toErrorSummaryItems(List<PositionValidationError> validationErrors) {
    var items = new ArrayList<ErrorSummaryItem>();
    for (var error : validationErrors.reversed()) {
      items.add(new ErrorSummaryItem(
          items.size(),
          error.positionId().toString(),
          "%s - %s".formatted(error.positionName(), error.message())
      ));
    }
    return items;
  }
}
