package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.SelectLicenceWorkAmendmentController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionSubmissionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose.SwpApplicationRequestPurpose;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose.SwpApplicationRequestPurposeController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose.SwpApplicationRequestPurposeRepository;
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
  private LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationTaskListSectionService scheduleWorkProgrammeApplicationTaskListSectionService;

  @Test
  void getSection() {
    var application = new ScheduleWorkProgrammeApplicationDetail();
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

  @Test
  void getSectionWithExtensionComplete() {
    SwpApplicationRequestPurpose swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setExtendTerm(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceScheduleExtensionSubmissionService.isSectionSubmittable(any())).thenReturn(true);

    var application = new ScheduleWorkProgrammeApplicationDetail();
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
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTENSION_DETAILS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleExtensionController.class).renderForm(null, null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSectionWithExtensionNotComplete() {
    SwpApplicationRequestPurpose swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setExtendTerm(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceScheduleExtensionSubmissionService.isSectionSubmittable(any())).thenReturn(false);

    var application = new ScheduleWorkProgrammeApplicationDetail();
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
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.EXTENSION_DETAILS,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(LicenceScheduleExtensionController.class).renderForm(null, null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSectionWithAmendmentComplete() {
    SwpApplicationRequestPurpose swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(any())).thenReturn(true);

    var application = new ScheduleWorkProgrammeApplicationDetail();
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
                    ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(
                        null,
                        null))
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.AMENDMENT_DETAILS,
                    TaskListLabel.COMPLETE,
                    ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class).renderForm(
                        null,
                        null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }

  @Test
  void getSectionWithAmendmentNotComplete() {
    SwpApplicationRequestPurpose swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setAmendWorkProgramme(true);
    when(swpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail(any())).thenReturn(
        Optional.of(swpApplicationRequestPurpose));
    when(licenceWorkProgrammeAmendmentSubmissionService.isAmendmentSectionSubmittable(any())).thenReturn(false);

    var application = new ScheduleWorkProgrammeApplicationDetail();
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
                ),
                new TaskListItem(
                    ScheduleWorkProgrammeApplicationTaskListSectionService.AMENDMENT_DETAILS,
                    TaskListLabel.NOT_COMPLETE,
                    ReverseRouter.route(on(SelectLicenceWorkAmendmentController.class).renderForm(null, null))
                )
            ),
            ScheduleWorkProgrammeApplicationTaskListSectionService.APPLICATION_DETAILS_SECTION_NAME,
            ScheduleWorkProgrammeApplicationTaskListSectionService.SECTION_ORDER
        );
  }
}