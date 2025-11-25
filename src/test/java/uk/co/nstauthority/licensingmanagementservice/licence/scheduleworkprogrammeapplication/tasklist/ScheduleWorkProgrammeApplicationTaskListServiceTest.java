package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

  @Mock
  private LicenceScheduleReviewAndSubmitTaskListSectionService licenceScheduleReviewAndSubmitTaskListSectionService;

  private ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  @BeforeEach
  void setUp() {
    List<TaskListSectionService<ScheduleWorkProgrammeApplicationDetail>> taskListSections = List.of(
        scheduleWorkProgrammeApplicationTaskListSectionService,
        licenceScheduleReviewAndSubmitTaskListSectionService
    );
    scheduleWorkProgrammeApplicationTaskListService = new ScheduleWorkProgrammeApplicationTaskListService(taskListSections);
  }

  @Test
  void getAllSections() {
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var section1 = Optional.of(new TaskListSection("Application details", 10, List.of()));
    when(scheduleWorkProgrammeApplicationTaskListSectionService.getSection(applicationDetail, user)).thenReturn(section1);
    var section2 = Optional.of(new TaskListSection("Review and submit", 20, List.of()));
    when(licenceScheduleReviewAndSubmitTaskListSectionService.getSection(applicationDetail, user)).thenReturn(section2);

    var sections = scheduleWorkProgrammeApplicationTaskListService.getAllSections(applicationDetail, user);
    assertThat(sections)
        .containsExactly(
            section1.get(),
            section2.get()
        );
  }

  @Test
  void isSubmittable_allSectionsCompleted_returnsTrue() {
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    var user = ServiceUserDetailTestUtil.newBuilder().build();

    TaskListSection section1 = mock(TaskListSection.class);
    when(section1.isCompleted()).thenReturn(true);
    TaskListSection section2 = mock(TaskListSection.class);
    when(section2.isCompleted()).thenReturn(true);

    when(scheduleWorkProgrammeApplicationTaskListSectionService.getSection(applicationDetail, user)).thenReturn(Optional.of(section1));
    when(licenceScheduleReviewAndSubmitTaskListSectionService.getSection(applicationDetail, user)).thenReturn(Optional.of(section2));

    boolean result = scheduleWorkProgrammeApplicationTaskListService.isSubmittable(applicationDetail, user);

    assertThat(result).isTrue();
  }

  @Test
  void isSubmittable_sectionNotCompleted_returnsFalse() {
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    var user = ServiceUserDetailTestUtil.newBuilder().build();

    TaskListSection section1 = mock(TaskListSection.class);
    when(section1.isCompleted()).thenReturn(true);
    TaskListSection section2 = mock(TaskListSection.class);
    when(section2.isCompleted()).thenReturn(false);

    when(scheduleWorkProgrammeApplicationTaskListSectionService.getSection(applicationDetail, user)).thenReturn(Optional.of(section1));
    when(licenceScheduleReviewAndSubmitTaskListSectionService.getSection(applicationDetail, user)).thenReturn(Optional.of(section2));

    boolean result = scheduleWorkProgrammeApplicationTaskListService.isSubmittable(applicationDetail, user);

    assertThat(result).isFalse();
  }
}
