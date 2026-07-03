package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

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

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = EventCommentDeletionController.class)
class EventCommentDeletionControllerTest extends AbstractControllerTest {

  private static final Set<Role> PERMITTED_ROLES = Set.of(
      Role.SCHEDULE_ADMINISTRATOR,
      Role.WORK_PROGRAMME_ADMINISTRATOR,
      Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR
  );

  private static final String LICENCE_CAPTION = "licence caption";

  @MockitoBean
  private EventCommentService eventCommentService;

  private EventComment eventComment;

  private EventCommentView commentView;

  @BeforeEach
  void setUp() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    var scheduleEvent = new LicenceScheduleTerm();
    scheduleEvent.setId(UUID.randomUUID());
    scheduleEvent.setLicenceSchedule(licenceSchedule);

    eventComment = new EventComment();
    eventComment.setId(UUID.randomUUID());
    eventComment.setScheduleEvent(scheduleEvent);

    commentView = new EventCommentView("comment text", "Author Name", "1 January 2025 10:00:00", "");
  }

  @Test
  void renderDeleteCommentPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(true);
    when(eventCommentService.getEventCommentByIdOrThrow(eventComment.getId()))
        .thenReturn(eventComment);
    when(eventCommentService.getEventCommentViewFor(eventComment))
        .thenReturn(commentView);
    when(licenceService.getLicencePageCaption(eventComment.getScheduleEvent().getLicenceSchedule().getLicence()))
        .thenReturn(LICENCE_CAPTION);

    mockMvc.perform(
            get(ReverseRouter.route(on(EventCommentDeletionController.class)
                .renderDeleteCommentPage(eventComment.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteEventComment"))
        .andExpect(model().attribute("commentView", commentView))
        .andExpect(model().attribute("pageCaption", LICENCE_CAPTION))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(eventComment.getScheduleEvent().getLicenceSchedule().getLicence().getId(), null, null, null))));
  }

  @Test
  void renderDeleteCommentPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(EventCommentDeletionController.class)
                .renderDeleteCommentPage(eventComment.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitDeleteCommentPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(true);
    when(eventCommentService.getEventCommentByIdOrThrow(eventComment.getId()))
        .thenReturn(eventComment);

    mockMvc.perform(
            post(ReverseRouter.route(on(EventCommentDeletionController.class)
                .submitDeleteCommentPage(eventComment.getId(), null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(eventCommentService).deleteEventComment(eventComment);
  }

  @Test
  void submitDeleteCommentPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(EventCommentDeletionController.class)
                .submitDeleteCommentPage(eventComment.getId(), null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(eventCommentService, never()).deleteEventComment(eventComment);
  }
}
