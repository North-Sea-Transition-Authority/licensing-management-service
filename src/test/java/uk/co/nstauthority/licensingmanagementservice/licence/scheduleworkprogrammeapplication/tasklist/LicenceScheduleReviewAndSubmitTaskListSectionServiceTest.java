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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.LicenceScheduleReviewAndSubmitController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleReviewAndSubmitTaskListSectionServiceTest {

  @InjectMocks
  private LicenceScheduleReviewAndSubmitTaskListSectionService licenceScheduleReviewAndSubmitTaskListSectionService;

  @Test
  void getSection() {

    var application = new ScheduleWorkProgrammeApplicationDetail();
    var user = ServiceUserDetailTestUtil
        .newBuilder()
        .build();
    var sectionOptional = licenceScheduleReviewAndSubmitTaskListSectionService.getSection(
        application,
        user
    );

    assertThat(sectionOptional).isPresent();

    var section = sectionOptional.get();

    assertThat(section)
        .extracting(
            TaskListSection::items,
            TaskListSection::displayName,
            TaskListSection::displayOrder
        )
        .containsExactly(
            List.of(new TaskListItem(
                LicenceScheduleReviewAndSubmitTaskListSectionService.REVIEW_AND_SUBMIT,
                ReverseRouter.route(on(LicenceScheduleReviewAndSubmitController.class).getReviewAndSubmit(
                    null,
                    null
                ))
            )),
            LicenceScheduleReviewAndSubmitTaskListSectionService.REVIEW_AND_SUBMIT,
            LicenceScheduleReviewAndSubmitTaskListSectionService.SECTION_ORDER
        );
  }
}