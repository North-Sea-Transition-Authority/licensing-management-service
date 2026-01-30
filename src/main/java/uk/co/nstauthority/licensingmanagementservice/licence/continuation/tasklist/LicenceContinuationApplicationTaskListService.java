package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class LicenceContinuationApplicationTaskListService {

  private final List<TaskListSectionService<LicenceContinuationApplicationDetail>> taskListSectionServices;

  @Autowired
  public LicenceContinuationApplicationTaskListService(
      List<TaskListSectionService<LicenceContinuationApplicationDetail>> taskListSectionServices) {
    this.taskListSectionServices = taskListSectionServices;
  }

  public List<TaskListSection> getAllSections(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
                                              ServiceUserDetail user) {
    return taskListSectionServices.stream()
        .map(tlss -> tlss.getSection(licenceContinuationApplicationDetail, user))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(TaskListSection::displayOrder))
        .toList();
  }

  public boolean isSubmittable(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
                               ServiceUserDetail user) {
    return getAllSections(licenceContinuationApplicationDetail, user)
        .stream()
        .allMatch(TaskListSection::isCompleted);
  }
}
