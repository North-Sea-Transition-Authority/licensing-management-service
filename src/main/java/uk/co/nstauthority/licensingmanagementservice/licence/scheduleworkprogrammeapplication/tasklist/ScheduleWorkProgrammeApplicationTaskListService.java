package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class ScheduleWorkProgrammeApplicationTaskListService {

  private final List<TaskListSectionService<ScheduleWorkProgrammeApplicationDetail>> taskListSectionServices;

  @Autowired
  public ScheduleWorkProgrammeApplicationTaskListService(
      List<TaskListSectionService<ScheduleWorkProgrammeApplicationDetail>> taskListSectionServices) {
    this.taskListSectionServices = taskListSectionServices;
  }

  public List<TaskListSection> getAllSections(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplication,
                                              ServiceUserDetail user) {
    return taskListSectionServices.stream()
        .map(tlss -> tlss.getSection(scheduleWorkProgrammeApplication, user))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(TaskListSection::displayOrder))
        .toList();
  }

  public boolean isSubmittable(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplication,
                               ServiceUserDetail user) {
    return getAllSections(scheduleWorkProgrammeApplication, user)
        .stream()
        .allMatch(TaskListSection::isCompleted);
  }
}
