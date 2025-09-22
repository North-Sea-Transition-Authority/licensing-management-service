package uk.co.nstauthority.licensingmanagementservice.xyzapplication.tasklist;

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
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationStatus;

@ExtendWith(MockitoExtension.class)
class XyzApplicationTaskListServiceTest {

  @Mock
  private XyzApplicationTaskListSectionService xyzApplicationTaskListSectionService;

  private XyzApplicationTaskListService xyzApplicationTaskListService;

  @BeforeEach
  void setUp() {
    List<XyzTaskListSectionService<XyzApplication>> taskListSections = List.of(xyzApplicationTaskListSectionService);
    xyzApplicationTaskListService = new XyzApplicationTaskListService(taskListSections);
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
