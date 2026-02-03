package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaRequirementController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationRequirementsTaskListSectionServiceTest {

  @Mock
  private LicenceContinuationWpaSubmissionService licenceContinuationWpaSubmissionService;

  @InjectMocks
  private LicenceContinuationRequirementsTaskListSectionService licenceContinuationRequirementsTaskListSectionService;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private ServiceUserDetail user;

  @BeforeEach
  void setUp() {
    user = ServiceUserDetailTestUtil.newBuilder().build();
    licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    licenceContinuationApplicationDetail.setId(UUID.randomUUID());
  }

  @Test
  void getSection_WhenNotSubmittable_ReturnsNotStartedLabel() {
    when(licenceContinuationWpaSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);

    var sectionOptional = licenceContinuationRequirementsTaskListSectionService.getSection(
        licenceContinuationApplicationDetail, user);

    assertThat(sectionOptional).isPresent();
    var section = sectionOptional.get();

    assertThat(section)
        .extracting(
            TaskListSection::displayName,
            TaskListSection::displayOrder
        )
        .containsExactly(
            LicenceContinuationRequirementsTaskListSectionService.LICENCE_CONTINUATION_REQUIREMENTS_SECTION_NAME,
            LicenceContinuationRequirementsTaskListSectionService.SECTION_ORDER
        );

    assertThat(section.items())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.WORK_PROGRAMMES,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationWpaRequirementController.class)
                                        .renderForm(licenceContinuationApplicationDetail.getId(), null))
            )
        );
  }

  @Test
  void getSection_WhenSubmittable_ReturnsCompleteLabel() {
    when(licenceContinuationWpaSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(true);

    var sectionOptional = licenceContinuationRequirementsTaskListSectionService.getSection(
        licenceContinuationApplicationDetail, user);

    assertThat(sectionOptional).isPresent();
    var section = sectionOptional.get();

    assertThat(section.items())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.WORK_PROGRAMMES,
                TaskListLabel.COMPLETE,
                ReverseRouter.route(on(LicenceContinuationWpaRequirementController.class)
                                        .renderForm(licenceContinuationApplicationDetail.getId(), null))
            )
        );
  }
}