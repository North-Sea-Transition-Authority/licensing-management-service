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
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationLicenceOperatorsController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationLicenceOperatorsSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaRequirementController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.OtherRequirementsVisibility;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.OtherRequirementsVisibilityResolverService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationRequirementsTaskListSectionServiceTest {

  @Mock
  private LicenceContinuationLicenceOperatorsSubmissionService licenceContinuationLicenceOperatorsSubmissionService;

  @Mock
  private LicenceContinuationWpaSubmissionService licenceContinuationWpaSubmissionService;

  @Mock
  private LicenceContinuationOtherRequirementSubmissionService licenceContinuationOtherRequirementSubmissionService;

  @Mock
  private OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService;

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @InjectMocks
  private LicenceContinuationRequirementsTaskListSectionService licenceContinuationRequirementsTaskListSectionService;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private ServiceUserDetail user;
  private OtherRequirementsVisibility visibilityWithRequirements;
  private OtherRequirementsVisibility visibilityNoRequirements;
  private LicenceScheduleDetail scheduleDetail;

  @BeforeEach
  void setUp() {
    user = ServiceUserDetailTestUtil.newBuilder().build();
    licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    licenceContinuationApplicationDetail.setId(UUID.randomUUID());

    visibilityWithRequirements = new OtherRequirementsVisibility(true, false, false);
    visibilityNoRequirements = new OtherRequirementsVisibility(false, false, false);

    scheduleDetail = new LicenceScheduleDetail();
    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(scheduleDetail);
  }

  @Test
  void getSection_WhenNotSubmittable_ReturnsNotStartedLabel() {
    when(otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail))
        .thenReturn(visibilityWithRequirements);
    when(workProgrammeActivityService.hasCurrentWorkProgrammeActivities(scheduleDetail))
        .thenReturn(true);

    when(licenceContinuationLicenceOperatorsSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);
    when(licenceContinuationWpaSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);
    when(licenceContinuationOtherRequirementSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
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
            ),
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.OTHER_REQUIREMENTS,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class)
                                        .renderForm(licenceContinuationApplicationDetail.getId(), null))
            ),
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.LICENCE_OPERATORS,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class).renderForm(
                    licenceContinuationApplicationDetail.getId(),
                    null
                ))
            )
        );
  }

  @Test
  void getSection_WhenSubmittable_ReturnsCompleteLabels() {
    when(otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail))
        .thenReturn(visibilityWithRequirements);
    when(workProgrammeActivityService.hasCurrentWorkProgrammeActivities(scheduleDetail))
        .thenReturn(true);

    when(licenceContinuationLicenceOperatorsSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(true);
    when(licenceContinuationWpaSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(true);
    when(licenceContinuationOtherRequirementSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
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
            ),
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.OTHER_REQUIREMENTS,
                TaskListLabel.COMPLETE,
                ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class)
                                        .renderForm(licenceContinuationApplicationDetail.getId(), null))
            ),
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.LICENCE_OPERATORS,
                TaskListLabel.COMPLETE,
                ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class).renderForm(
                    licenceContinuationApplicationDetail.getId(),
                    null
                ))
            )
        );
  }

  @Test
  void getSection_MixedStatus_ReturnsCorrectLabels() {
    when(otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail))
        .thenReturn(visibilityWithRequirements);
    when(workProgrammeActivityService.hasCurrentWorkProgrammeActivities(scheduleDetail))
        .thenReturn(true);

    when(licenceContinuationLicenceOperatorsSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);
    when(licenceContinuationWpaSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(true);
    when(licenceContinuationOtherRequirementSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);

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
            ),
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.OTHER_REQUIREMENTS,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class)
                                        .renderForm(licenceContinuationApplicationDetail.getId(), null))
            ),
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.LICENCE_OPERATORS,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class).renderForm(
                    licenceContinuationApplicationDetail.getId(),
                    null
                ))
            )
        );
  }

  @Test
  void getSection_whenNoWorkProgrammeActivities_omitsWorkProgrammesTask() {
    when(otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail))
        .thenReturn(visibilityWithRequirements);
    when(workProgrammeActivityService.hasCurrentWorkProgrammeActivities(scheduleDetail))
        .thenReturn(false);

    when(licenceContinuationLicenceOperatorsSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);
    when(licenceContinuationOtherRequirementSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);

    var sectionOptional = licenceContinuationRequirementsTaskListSectionService.getSection(
        licenceContinuationApplicationDetail, user);

    assertThat(sectionOptional).isPresent();
    var section = sectionOptional.get();

    assertThat(section.items())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.OTHER_REQUIREMENTS,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class)
                                        .renderForm(licenceContinuationApplicationDetail.getId(), null))
            ),
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.LICENCE_OPERATORS,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class).renderForm(
                    licenceContinuationApplicationDetail.getId(),
                    null
                ))
            )
        );

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .doesNotContain(LicenceContinuationRequirementsTaskListSectionService.WORK_PROGRAMMES);
  }

  @Test
  void getSection_WhenNoOtherRequirements_OmitsOtherRequirementsTask() {
    when(otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail))
        .thenReturn(visibilityNoRequirements);
    when(workProgrammeActivityService.hasCurrentWorkProgrammeActivities(scheduleDetail))
        .thenReturn(true);

    when(licenceContinuationLicenceOperatorsSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);
    when(licenceContinuationWpaSubmissionService.isSectionSubmittable(licenceContinuationApplicationDetail))
        .thenReturn(false);

    var sectionOptional = licenceContinuationRequirementsTaskListSectionService.getSection(
        licenceContinuationApplicationDetail, user);

    assertThat(sectionOptional).isPresent();
    var section = sectionOptional.get();

    assertThat(section.items())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.WORK_PROGRAMMES,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationWpaRequirementController.class)
                                        .renderForm(licenceContinuationApplicationDetail.getId(), null))
            ),
            new TaskListItem(
                LicenceContinuationRequirementsTaskListSectionService.LICENCE_OPERATORS,
                TaskListLabel.NOT_COMPLETE,
                ReverseRouter.route(on(LicenceContinuationLicenceOperatorsController.class).renderForm(
                    licenceContinuationApplicationDetail.getId(),
                    null
                ))
            )
        );
  }
}