package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = WorkProgrammeActivityDeletionController.class)
class WorkProgrammeActivityDeletionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private EventCommentService eventCommentService;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  private WorkProgrammeActivity workProgrammeActivity;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setId(UUID.randomUUID());
    workProgrammeActivity.setLicenceScheduleDetail(licenceScheduleDetail);
    workProgrammeActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    workProgrammeActivity.setDescription("description");
    workProgrammeActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    workProgrammeActivity.setDueDate(LocalDate.of(2025, 1, 1));
  }

  @Test
  void renderDeleteActivityPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId())).thenReturn(workProgrammeActivity);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class).renderDeleteActivityPage(workProgrammeActivity.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteWorkProgrammeActivity"))
        .andExpect(model().attribute("pageTitle", "Do you want to delete the %s activity?".formatted(workProgrammeActivity.getCategoryString())))
        .andExpect(model().attribute("summaryView", WorkProgrammeActivitySummaryView.fromWorkProgrammeActivity(workProgrammeActivity)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", "caption"))
        .andExpect(model().attribute("pendingComment", ""));
  }

  @Test
  void renderDeleteActivityPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class).renderDeleteActivityPage(workProgrammeActivity.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitDeleteActivityPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId())).thenReturn(workProgrammeActivity);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class).submitDeleteActivityPage(workProgrammeActivity.getId(), null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(workProgrammeActivityService).deleteWorkProgrammeActivity(workProgrammeActivity);
  }

  @Test
  void submitDeleteActivityPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class).submitDeleteActivityPage(workProgrammeActivity.getId(), null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(workProgrammeActivityService, never()).deleteWorkProgrammeActivity(workProgrammeActivity);
  }

  @Test
  void renderDeleteActivityPage_whenActivityHasEventReferenceAndPendingCommentExists_showsPendingComment() throws Exception {
    var eventReference = new EventReference();
    workProgrammeActivity.setEventReference(eventReference);

    var eventComment = new EventComment();
    eventComment.setComment("a pending comment");

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId())).thenReturn(workProgrammeActivity);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.of(eventComment));

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class).renderDeleteActivityPage(workProgrammeActivity.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", "a pending comment"));
  }

  @Test
  void renderDeleteActivityPage_whenActivityHasEventReferenceAndNoPendingComment_showsEmptyPendingComment() throws Exception {
    var eventReference = new EventReference();
    workProgrammeActivity.setEventReference(eventReference);

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId())).thenReturn(workProgrammeActivity);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.empty());

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class).renderDeleteActivityPage(workProgrammeActivity.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", ""));
  }

}