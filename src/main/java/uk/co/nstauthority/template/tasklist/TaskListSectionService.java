package uk.co.nstauthority.template.tasklist;

import java.util.Optional;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;

public interface TaskListSectionService<T> {

  Optional<TaskListSection> getSection(T section, ServiceUserDetail user);
}
