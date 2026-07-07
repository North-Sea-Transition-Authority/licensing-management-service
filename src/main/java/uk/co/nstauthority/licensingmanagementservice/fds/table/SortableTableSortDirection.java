package uk.co.nstauthority.licensingmanagementservice.fds.table;

public enum SortableTableSortDirection {
  ASCENDING(1), DESCENDING(-1), NONE(0);

  private final int frontendSortValue;

  SortableTableSortDirection(int frontendSortValue) {
    this.frontendSortValue = frontendSortValue;
  }

  public int getFrontendSortValue() {
    return frontendSortValue;
  }
}
