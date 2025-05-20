package uk.co.nstauthority.template.tasklist;

import java.util.List;

public record TaskListSection(
    String displayName,
    int displayOrder,
    List<TaskListItem> items
) { }
