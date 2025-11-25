package uk.co.nstauthority.licensingmanagementservice.tasklist;

import java.util.List;
import java.util.Objects;

public record TaskListSection(
    String displayName,
    int displayOrder,
    List<TaskListItem> items
) {
  public boolean isCompleted() {

    return items()
        .stream()
        .map(TaskListItem::label)
        .filter(Objects::nonNull)
        .allMatch(TaskListLabel.COMPLETE::equals);
  }
}
