package uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceScheduleTabController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = ScheduleCommentRemovalController.class)
class ScheduleCommentRemovalControllerTest extends AbstractControllerTest {

  private static final Set<Role> PERMITTED_ROLES = Set.of(
      Role.SCHEDULE_ADMINISTRATOR,
      Role.WORK_PROGRAMME_ADMINISTRATOR
  );

  private static final String LICENCE_CAPTION = "licence caption";

  @MockitoBean
  private ScheduleCommentService scheduleCommentService;

  private Licence licence;

  private ScheduleComment scheduleComment;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(1)
        .build();

    scheduleComment = new ScheduleComment();
    scheduleComment.setId(UUID.randomUUID());
    scheduleComment.setLicenceSchedule(LicenceScheduleTestUtil.createLicenceSchedule(licence));
  }

  @Test
  void renderRemoveCommentPage() throws Exception {
    givenUserHasPermittedRole(true);
    var commentView = new ScheduleCommentView("comment text", "", "1 January 2025 10:00:00", "");
    when(scheduleCommentService.getCommentOrThrow(scheduleComment.getId())).thenReturn(scheduleComment);
    when(scheduleCommentService.getCommentView(scheduleComment)).thenReturn(commentView);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(LICENCE_CAPTION);

    mockMvc.perform(get(removeUrl()).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/removeScheduleComment"))
        .andExpect(model().attribute("commentView", commentView))
        .andExpect(model().attribute("pageCaption", LICENCE_CAPTION))
        .andExpect(model().attribute("cancelUrl", scheduleTabUrl()));
  }

  @Test
  void renderRemoveCommentPage_noRoles() throws Exception {
    givenUserHasPermittedRole(false);

    mockMvc.perform(get(removeUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void removeComment() throws Exception {
    givenUserHasPermittedRole(true);
    when(scheduleCommentService.getCommentOrThrow(scheduleComment.getId())).thenReturn(scheduleComment);

    mockMvc.perform(post(removeUrl()).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(scheduleTabUrl()));

    verify(scheduleCommentService).removeComment(scheduleComment);
  }

  @Test
  void removeComment_noRoles() throws Exception {
    givenUserHasPermittedRole(false);

    mockMvc.perform(post(removeUrl()).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isForbidden());

    verify(scheduleCommentService, never()).removeComment(any());
  }

  private void givenUserHasPermittedRole(boolean hasRole) {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, PERMITTED_ROLES))
        .thenReturn(hasRole);
  }

  private String removeUrl() {
    return ReverseRouter.route(on(ScheduleCommentRemovalController.class)
        .renderRemoveCommentPage(scheduleComment.getId()));
  }

  private String scheduleTabUrl() {
    return ReverseRouter.route(on(LicenceScheduleTabController.class)
        .renderLicenceOverview(licence.getId(), null, null, null));
  }
}
