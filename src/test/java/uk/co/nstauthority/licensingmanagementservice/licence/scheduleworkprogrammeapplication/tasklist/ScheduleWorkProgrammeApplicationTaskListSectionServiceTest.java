package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.SelectLicenceWorkAmendmentController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurpose;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeRepository;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationTaskListSectionServiceTest {

  @Mock
  private SwpApplicationRequestPurposeRepository swpApplicationRequestPurposeRepository;

  @Mock
  private LicenceScheduleExtensionSubmissionService licenceScheduleExtensionSubmissionService;

  @Mock
  private LicenceWorkProgrammeAmendmentSubmissionService licenceWorkProgrammeAmendmentSubmissionService;

  @Mock
  private LicenceScheduleSupportingInformationSubmissionService licenceScheduleSupportingInformationSubmissionService;

  @Mock
  private TeamManagementService teamManagementService;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationTaskListSectionService scheduleWorkProgrammeApplicationTaskListSectionService;

  private ScheduleWorkProgrammeApplicationDetail application;
  private ServiceUserDetail user;
  private Team team;

  @BeforeEach
  void setUp() {
    user = ServiceUserDetailTestUtil.newBuilder().build();
    team = new Team(UUID.randomUUID());

    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setId(UUID.randomUUID());

    application = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .build();

    when(teamManagementService.getScopedTeam(any(), any())).thenReturn(Optional.of(team));
  }

  @Test
  void getSection() {
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
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(team.getId(), user))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.WHAT_ARE_YOU_REQUESTING_TO_DO,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(application.getId(), null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSection_withExtensionComplete() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setExtendTerm(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceScheduleExtensionSubmissionService.isSectionSubmittable(any())).thenReturn(true);
    when(licenceScheduleSupportingInformationSubmissionService.isSectionSubmittable(any())).thenReturn(true);

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
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(team.getId(), user))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.WHAT_ARE_YOU_REQUESTING_TO_DO,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTENSION_DETAILS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleExtensionController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.SUPPORTING_INFORMATION,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).renderForm(application.getId(), null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSection_withExtensionNotComplete() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setExtendTerm(true);

    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceScheduleExtensionSubmissionService.isSectionSubmittable(any())).thenReturn(false);

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
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(team.getId(), user))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.WHAT_ARE_YOU_REQUESTING_TO_DO,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTENSION_DETAILS,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleExtensionController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.SUPPORTING_INFORMATION,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).renderForm(application.getId(), null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSection_withAmendmentSubmittableAndComplete() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(any())).thenReturn(true);
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionComplete(any())).thenReturn(true);

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
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(team.getId(), user))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.WHAT_ARE_YOU_REQUESTING_TO_DO,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.AMENDMENT_DETAILS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class).renderForm(application.getId(), application))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.SUPPORTING_INFORMATION,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).renderForm(application.getId(), null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSection_withAmendmentNotSubmittableOrComplete() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(any())).thenReturn(false);
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionComplete(any())).thenReturn(false);

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
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(team.getId(), user))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.WHAT_ARE_YOU_REQUESTING_TO_DO,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.AMENDMENT_DETAILS,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(SelectLicenceWorkAmendmentController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.SUPPORTING_INFORMATION,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).renderForm(application.getId(), null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSection_withAmendmentSubmittableButNotComplete() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(any())).thenReturn(true);
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionComplete(any())).thenReturn(false);

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
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(team.getId(), user))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.WHAT_ARE_YOU_REQUESTING_TO_DO,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.AMENDMENT_DETAILS,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.SUPPORTING_INFORMATION,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).renderForm(application.getId(), null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSection_withSupportingInformationComplete() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(any())).thenReturn(true);
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionComplete(any())).thenReturn(true);
    when(licenceScheduleSupportingInformationSubmissionService.isSectionSubmittable(any())).thenReturn(true);

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
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(TeamManagementController.class).renderExternalContributorsTeamList(team.getId(), user))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.WHAT_ARE_YOU_REQUESTING_TO_DO,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.AMENDMENT_DETAILS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class).renderForm(application.getId(), null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.SUPPORTING_INFORMATION,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).renderForm(application.getId(), null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }
}