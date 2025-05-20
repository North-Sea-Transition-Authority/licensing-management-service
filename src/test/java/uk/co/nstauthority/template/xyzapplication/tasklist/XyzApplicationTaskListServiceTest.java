package uk.co.nstauthority.template.xyzapplication.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.template.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.template.tasklist.TaskListSection;
import uk.co.nstauthority.template.tasklist.TaskListSectionService;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationStatus;

@ExtendWith(MockitoExtension.class)
class XyzApplicationTaskListServiceTest {

  private List<TaskListSectionService<XyzApplication>> taskListSections;

  @Mock
  private XyzApplicationTaskListSectionService xyzApplicationTaskListSectionService;

  private XyzApplicationTaskListService xyzApplicationTaskListService;

  @BeforeEach
  void setUp() {
    xyzApplicationTaskListService = new XyzApplicationTaskListService(List.of(xyzApplicationTaskListSectionService));
  }

  @Test
  void getAllSections() {
    var application = new XyzApplication(UUID.randomUUID(), "ref", "type", XyzApplicationStatus.DRAFT);
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var section1 = Optional.of(new TaskListSection("Application details", 10, List.of()));
    when(xyzApplicationTaskListSectionService.getSection(application, user)).thenReturn(section1);

    var sections = xyzApplicationTaskListService.getAllSections(application, user);
    assertThat(sections).containsExactly(section1.get());
  }
}
