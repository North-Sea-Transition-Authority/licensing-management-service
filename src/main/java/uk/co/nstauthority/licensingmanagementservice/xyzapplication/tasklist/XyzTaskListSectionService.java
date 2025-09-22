package uk.co.nstauthority.licensingmanagementservice.xyzapplication.tasklist;

import java.util.Optional;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

public interface XyzTaskListSectionService<T> {

  Optional<TaskListSection> getSection(T section, ServiceUserDetail user);
}