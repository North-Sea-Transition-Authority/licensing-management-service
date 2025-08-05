package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationTaskListServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationTaskListSectionService scheduleWorkProgrammeApplicationTaskListSectionService;

  private ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  @BeforeEach
  void setUp() {
    List<TaskListSectionService<ScheduleWorkProgrammeApplicationDetail>> taskListSections = List.of(scheduleWorkProgrammeApplicationTaskListSectionService);
    scheduleWorkProgrammeApplicationTaskListService = new ScheduleWorkProgrammeApplicationTaskListService(taskListSections);
  }

  @Test
  void getAllSections() {
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var section1 = Optional.of(new TaskListSection("Application details", 10, List.of()));
    when(scheduleWorkProgrammeApplicationTaskListSectionService.getSection(applicationDetail, user)).thenReturn(section1);

    var sections = scheduleWorkProgrammeApplicationTaskListService.getAllSections(applicationDetail, user);
    assertThat(sections).containsExactly(section1.get());
  }
}
