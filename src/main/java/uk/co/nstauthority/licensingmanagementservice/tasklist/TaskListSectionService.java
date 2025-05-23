package uk.co.nstauthority.licensingmanagementservice.tasklist;

import java.util.Optional;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;

public interface TaskListSectionService<T> {

  Optional<TaskListSection> getSection(T section, ServiceUserDetail user);
}
