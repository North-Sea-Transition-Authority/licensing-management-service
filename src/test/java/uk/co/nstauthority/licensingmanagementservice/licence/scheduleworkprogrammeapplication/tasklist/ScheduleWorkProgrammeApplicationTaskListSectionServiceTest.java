package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose.SwpApplicationRequestPurposeController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationTaskListSectionServiceTest {

  @InjectMocks
  private ScheduleWorkProgrammeApplicationTaskListSectionService scheduleWorkProgrammeApplicationTaskListSectionService;

  @Test
  void getSection() {
    var application  = new ScheduleWorkProgrammeApplicationDetail();
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var sectionOptional = scheduleWorkProgrammeApplicationTaskListSectionService.getSection(application, user);
    assertThat(sectionOptional).isPresent();
    var section = sectionOptional.get();

    assertThat(section)
        .extracting(
            TaskListSection::items,
            TaskListSection::displayName,
            TaskListSection::displayOrder
        )
        .containsExactly(
            List.of(
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.WHAT_ARE_YOU_REQUESTING_TO_DO,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(null, null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }
}
