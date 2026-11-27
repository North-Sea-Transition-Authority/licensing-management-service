package uk.co.nstauthority.licensingmanagementservice.fds.error;

import java.util.List;
import java.util.stream.IntStream;

// This can't be a record because FDS calls it via getters
public class ErrorSummaryItem {

  private final int displayOrder;

  private final String fieldName;

  private final String errorMessage;

  public ErrorSummaryItem(int displayOrder, String fieldName, String errorMessage) {
    this.displayOrder = displayOrder;
    this.fieldName = fieldName;
    this.errorMessage = errorMessage;
  }

  public static List<ErrorSummaryItem> fromMessages(List<String> errorMessages, String fieldName) {
    return IntStream.range(0, errorMessages.size())
        .mapToObj(index -> new ErrorSummaryItem(index, fieldName, errorMessages.get(index)))
        .toList();
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
