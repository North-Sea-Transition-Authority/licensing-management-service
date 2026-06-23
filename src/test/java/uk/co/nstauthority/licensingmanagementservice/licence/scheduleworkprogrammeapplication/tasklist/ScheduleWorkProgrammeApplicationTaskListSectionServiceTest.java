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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.AmendmentSectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.SelectLicenceWorkAmendmentController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney.ScheduleWorkProgrammeExternalContributorController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney.ScheduleWorkProgrammeExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurpose;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

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
  private ScheduleWorkProgrammeExternalContributorService scheduleWorkProgrammeExternalContributorService;

  @Mock
  private SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationTaskListSectionService scheduleWorkProgrammeApplicationTaskListSectionService;

  private ScheduleWorkProgrammeApplicationDetail application;
  private ServiceUserDetail user;

  @BeforeEach
  void setUp() {
    user = ServiceUserDetailTestUtil.newBuilder().build();

    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setId(UUID.randomUUID());

    application = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .build();

    when(scheduleWorkProgrammeExternalContributorService.isExternalContributorSectionComplete(application))
        .thenReturn(true);
  }

  @Test
  void getSection() {
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(any())).thenReturn(true);

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
                    ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).renderForm(application.getId(), null))
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
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(any())).thenReturn(true);
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
                    ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).renderForm(application.getId(), null))
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
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(any())).thenReturn(true);
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
                    ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).renderForm(application.getId(), null))
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
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(any())).thenReturn(true);
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.getAmendmentSectionStatus(any()))
        .thenReturn(new AmendmentSectionStatus(true, true));

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
                    ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).renderForm(application.getId(), null))
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
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(any())).thenReturn(true);
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.getAmendmentSectionStatus(any()))
        .thenReturn(new AmendmentSectionStatus(false, false));

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
                    ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).renderForm(application.getId(), null))
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
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(any())).thenReturn(true);
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.getAmendmentSectionStatus(any()))
        .thenReturn(new AmendmentSectionStatus(true, false));

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
                    ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).renderForm(application.getId(), null))
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
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(any())).thenReturn(true);
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.getAmendmentSectionStatus(any()))
        .thenReturn(new AmendmentSectionStatus(true, true));
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
                    ReverseRouter.route(on(ScheduleWorkProgrammeExternalContributorController.class).renderForm(application.getId(), null))
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

  @Test
  void getSection_whenNoAmendableActivities_omitsWhatAreYouRequestingToDoItem() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setExtendPhaseOrTerm(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(any())).thenReturn(false);

    var sectionOptional = scheduleWorkProgrammeApplicationTaskListSectionService.getSection(application, user);
    assertThat(sectionOptional).isPresent();
    var section = sectionOptional.get();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(
            ScheduleWorkProgrammeApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS,
            ScheduleWorkProgrammeApplicationTaskListSectionService.EXTENSION_DETAILS,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SUPPORTING_INFORMATION
        );
  }

  @Test
  void getSection_whenAmendmentChosenButNoAmendableActivities_omitsAmendmentItem() {
    var swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(any())).thenReturn(false);

    var sectionOptional = scheduleWorkProgrammeApplicationTaskListSectionService.getSection(application, user);
    assertThat(sectionOptional).isPresent();

    assertThat(sectionOptional.get().items())
        .extracting(TaskListItem::displayName)
        .doesNotContain(ScheduleWorkProgrammeApplicationTaskListSectionService.AMENDMENT_DETAILS)
        .containsExactly(ScheduleWorkProgrammeApplicationTaskListSectionService.EXTERNAL_CONTRIBUTORS);
  }
}