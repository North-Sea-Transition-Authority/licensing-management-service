package uk.co.nstauthority.template.xyzapplication.tasklist;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.tasklist.TaskListSection;
import uk.co.nstauthority.template.tasklist.TaskListSectionService;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;

@Service
public class XyzApplicationTaskListService {

  private final List<TaskListSectionService<XyzApplication>> taskListSectionServices;

  @Autowired
  public XyzApplicationTaskListService(List<TaskListSectionService<XyzApplication>> taskListSectionServices) {
    this.taskListSectionServices = taskListSectionServices;
  }

  public List<TaskListSection> getAllSections(XyzApplication xyzApplication, ServiceUserDetail user) {
    return taskListSectionServices.stream()
        .map(tlss -> tlss.getSection(xyzApplication, user))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(TaskListSection::displayOrder))
        .toList();
  }
}
