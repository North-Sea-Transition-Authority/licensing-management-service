package uk.co.nstauthority.template.fds.error;

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
