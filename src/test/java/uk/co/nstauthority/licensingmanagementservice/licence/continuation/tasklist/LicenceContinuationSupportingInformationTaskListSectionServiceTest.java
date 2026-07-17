package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationSupportingInformationTaskListSectionServiceTest {

  @Mock
  private LicenceContinuationSupportingInformationSubmissionService licenceContinuationSupportingInformationSubmissionService;

  @InjectMocks
  private LicenceContinuationSupportingInformationTaskListSectionService taskListSectionService;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private ServiceUserDetail user;

  @BeforeEach
  void setUp() {
    user = ServiceUserDetailTestUtil.newBuilder().build();

    var licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setId(UUID.randomUUID());

    this.licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withLicenceContinuationApplication(licenceContinuationApplication)
        .build();
  }

  @Test
  void getSection_whenSectionSubmittable_itemIsComplete() {
    when(licenceContinuationSupportingInformationSubmissionService
        .isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(true);

    var sectionOptional = taskListSectionService.getSection(licenceContinuationApplicationDetail, user);
    assertThat(sectionOptional).isPresent();

    assertThat(sectionOptional.get())
        .extracting(
            TaskListSection::items,
            TaskListSection::displayName,
            TaskListSection::displayOrder
        )
        .containsExactly(
            List.of(
                new TaskListItem(
                    LicenceContinuationSupportingInformationTaskListSectionService
                        .ADDITIONAL_SUPPORTING_INFORMATION_SECTION_NAME,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(LicenceContinuationSupportingInformationController.class)
                        .renderForm(licenceContinuationApplicationDetail.getId(), null))
                )
            ),
            LicenceContinuationSupportingInformationTaskListSectionService
                .ADDITIONAL_SUPPORTING_INFORMATION_SECTION_NAME,
            LicenceContinuationSupportingInformationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSection_whenSectionNotSubmittable_itemIsNotComplete() {
    when(licenceContinuationSupportingInformationSubmissionService
        .isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);

    var sectionOptional = taskListSectionService.getSection(licenceContinuationApplicationDetail, user);
    assertThat(sectionOptional).isPresent();

    assertThat(sectionOptional.get().items())
        .containsExactly(
            new TaskListItem(
                LicenceContinuationSupportingInformationTaskListSectionService
                    .ADDITIONAL_SUPPORTING_INFORMATION_SECTION_NAME,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationSupportingInformationController.class)
                    .renderForm(licenceContinuationApplicationDetail.getId(), null))
            )
        );
  }
}
