package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEventRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = EventCommentController.class)
class EventCommentControllerTest extends AbstractControllerTest {

  private static final Set<Role> PERMITTED_ROLES = Set.of(
      Role.SCHEDULE_ADMINISTRATOR,
      Role.WORK_PROGRAMME_ADMINISTRATOR,
      Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR
  );

  private static final String LICENCE_CAPTION = "page caption";

  private static final String PAGE_CAPTION = "%s - %s".formatted(LICENCE_CAPTION, TermType.INITIAL.getDisplayName());

  @MockitoBean
  private EventCommentService eventCommentService;

  @MockitoBean
  private EventCommentValidator eventCommentValidator;

  @MockitoBean
  private ScheduleEventRepository scheduleEventRepository;

  private LicenceScheduleTerm scheduleEvent;

  @BeforeEach
  void setUp() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    scheduleEvent = new LicenceScheduleTerm();
    scheduleEvent.setId(UUID.randomUUID());
    scheduleEvent.setLicenceSchedule(licenceSchedule);
    scheduleEvent.setTermType(TermType.INITIAL);
  }

  @Test
  void renderAddCommentForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(true);
    when(scheduleEventRepository.findById(scheduleEvent.getId()))
        .thenReturn(Optional.of(scheduleEvent));
    when(licenceService.getLicencePageCaption(any())).thenReturn(LICENCE_CAPTION);

    mockMvc.perform(
            get(ReverseRouter.route(on(EventCommentController.class)
                .renderAddCommentForm(scheduleEvent.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createEventComment"))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(scheduleEvent.getLicenceSchedule().getLicence().getId(), null, null, null))))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @Test
  void renderAddCommentForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(EventCommentController.class)
                .renderAddCommentForm(scheduleEvent.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitAddCommentForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(true);
    when(scheduleEventRepository.findById(scheduleEvent.getId()))
        .thenReturn(Optional.of(scheduleEvent));
    when(eventCommentValidator.isValid(any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(EventCommentController.class)
                .submitAddCommentForm(scheduleEvent.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(eventCommentService).addNewComment(any(), eq(scheduleEvent), any());
  }

  @Test
  void submitAddCommentForm_invalidForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(true);
    when(scheduleEventRepository.findById(scheduleEvent.getId()))
        .thenReturn(Optional.of(scheduleEvent));
    when(licenceService.getLicencePageCaption(any())).thenReturn(LICENCE_CAPTION);
    when(eventCommentValidator.isValid(any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(EventCommentController.class)
                .submitAddCommentForm(scheduleEvent.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createEventComment"))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));

    verify(eventCommentService, never()).addNewComment(any(), any(), any());
  }

  @Test
  void submitAddCommentForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(EventCommentController.class)
                .submitAddCommentForm(scheduleEvent.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(eventCommentService, never()).addNewComment(any(), any(), any());
  }
}
