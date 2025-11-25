package uk.co.nstauthority.licensingmanagementservice.tasklist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TaskListSectionTest {
  private TaskListSection taskListSection;

  @Test
  void isCompleted_whenItemsCompleted() {
    var completedTaskListItem1 = new TaskListItem("Completed task 1", TaskListLabel.COMPLETE, "completed-url");
    var completedTaskListItem2 = new TaskListItem("Completed task 2", TaskListLabel.COMPLETE, "completed-url");
    var unlabelledTaskListItem = new TaskListItem("Review and submit", null, "unlabeled-url");

    taskListSection = new TaskListSection("section name", 10, List.of(
        completedTaskListItem1,
        completedTaskListItem2,
        unlabelledTaskListItem
    ));

    assertThat(taskListSection.isCompleted()).isTrue();
  }

  @Test
  void isCompleted_whenItemsNotCompleted() {
    var completedTaskListItem = new TaskListItem("Completed task", TaskListLabel.COMPLETE, "completed-url");
    var notCompletedTaskListItem = new TaskListItem("Not completed task", TaskListLabel.NOT_COMPLETE, "not-completed-url");

    taskListSection = new TaskListSection("section name", 10, List.of(
        completedTaskListItem,
        notCompletedTaskListItem
    ));

    assertThat(taskListSection.isCompleted()).isFalse();
  }
}