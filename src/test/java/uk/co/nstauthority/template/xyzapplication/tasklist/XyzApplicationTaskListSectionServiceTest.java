package uk.co.nstauthority.template.xyzapplication.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.template.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.tasklist.TaskListItem;
import uk.co.nstauthority.template.tasklist.TaskListLabel;
import uk.co.nstauthority.template.tasklist.TaskListSection;
import uk.co.nstauthority.template.workarea.WorkAreaController;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;
import uk.co.nstauthority.template.xyzapplication.XyzApplicationStatus;
import uk.co.nstauthority.template.xyzapplication.form.XyzApplicationFormController;

@ExtendWith(MockitoExtension.class)
class XyzApplicationTaskListSectionServiceTest {

  @InjectMocks
  private XyzApplicationTaskListSectionService xyzApplicationTaskListSectionService;

  @Test
  void getSection() {
    var application  = new XyzApplication(UUID.randomUUID(), "ref", "type", XyzApplicationStatus.DRAFT);
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var sectionOptional = xyzApplicationTaskListSectionService.getSection(application, user);
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
                    XyzApplicationTaskListSectionService.APPLICATION_DETAILS_ITEM_NAME,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(XyzApplicationFormController.class).getForm(application.getId(), null, null))
                ),
                new TaskListItem(
                    XyzApplicationTaskListSectionService.OTHER_DETAILS_ITEM_NAME,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))
                )
            ),
            XyzApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            XyzApplicationTaskListSectionService.SECTION_ORDER
        );
  }
}
