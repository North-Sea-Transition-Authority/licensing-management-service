package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class PartialSurrenderTaskListService {

  private final List<TaskListSectionService<PartialSurrenderTaskListContext>> taskListSectionServices;

  @Autowired
  public PartialSurrenderTaskListService(
      List<TaskListSectionService<PartialSurrenderTaskListContext>> taskListSectionServices
  ) {
    this.taskListSectionServices = taskListSectionServices;
  }

  public List<TaskListSection> getTaskListSections(PartialSurrenderTaskListContext context, ServiceUserDetail user) {
    return taskListSectionServices.stream()
        .map(taskListSectionService -> taskListSectionService.getSection(context, user))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(TaskListSection::displayOrder))
        .toList();
  }
}
