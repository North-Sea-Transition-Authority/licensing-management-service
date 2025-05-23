package uk.co.nstauthority.licensingmanagementservice.feedback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationStatus;

@ContextConfiguration(classes = FeedbackController.class)
class FeedbackControllerTest extends AbstractControllerTest {

  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @MockitoBean
  private FeedbackService feedbackService;

  @MockitoBean
  private FeedbackFormValidator feedbackFormValidator;

  private ServiceUserDetail organisationUser;

  private XyzApplication xyzApplication;

  @BeforeEach
  void setup() {
    xyzApplication = new XyzApplication(
        UUID.randomUUID(),
        "testref",
        "type",
        XyzApplicationStatus.DRAFT
    );

    when(xyzApplicationService.getXyzApplicationById(xyzApplication.getId()))
        .thenReturn(xyzApplication);
    when(xyzApplicationService.findXyzApplicationById(xyzApplication.getId()))
        .thenReturn(Optional.ofNullable(xyzApplication));
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @SecurityTest
  void getFeedback_whenNotLoggedIn() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(FeedbackController.class).getFeedback(null))))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void getFeedback_assertModelProperties() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION))
    ).thenReturn(true);
    mockMvc.perform(get(ReverseRouter.route(on(FeedbackController.class)
            .getFeedback(null)))
            .with(user(organisationUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/feedback/feedback"))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("pageName", FeedbackController.PAGE_NAME))
        .andExpect(model().attribute(
            "actionUrl",
            ReverseRouter.route(on(FeedbackController.class).submitFeedback(null, null, null))))
        .andExpect(
            model().attribute("serviceRatings", DisplayableEnumOptionUtil.getDisplayableOptions(ServiceFeedbackRating.class)))
        .andReturn().getModelAndView();
  }

  @SecurityTest
  void submitFeedback_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(FeedbackController.class)
            .submitFeedback(null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void submitFeedback_assertRedirect() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION))
    ).thenReturn(true);
    mockMvc.perform(post(ReverseRouter.route(on(FeedbackController.class)
            .submitFeedback(null, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))
        ));

    verify(feedbackService).saveFeedback(any(), any(), eq(organisationUser));
  }

  @Test
  void submitFeedback_whenHasErrors_assertOk() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION))
    ).thenReturn(true);
    doAnswer(invocation -> {
      var bindingResult = (BindingResult) invocation.getArgument(1);
      bindingResult.addError(new ObjectError("error", "error"));

      return invocation;
    }).when(feedbackFormValidator).validate(any(), any());

    mockMvc.perform(post(ReverseRouter.route(on(FeedbackController.class)
            .submitFeedback(null, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/feedback/feedback"));

    verify(feedbackService, never()).saveFeedback(any(), any(), any());
  }

  @SecurityTest
  void getApplicationFeedback_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(FeedbackController.class)
            .getApplicationFeedback(xyzApplication, null))))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void getApplicationFeedback_assertModelProperties() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION))
    ).thenReturn(true);
    mockMvc.perform(get(ReverseRouter.route(on(FeedbackController.class)
            .getApplicationFeedback(xyzApplication, null)))
            .with(user(organisationUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/feedback/feedback"))
        .andExpect(model().attributeExists("form"))
        .andExpect(model().attribute("pageName", FeedbackController.PAGE_NAME))
        .andExpect(model().attribute(
            "actionUrl",
            ReverseRouter.route(on(FeedbackController.class).submitApplicationFeedback(xyzApplication, null, null, null))
        ))
        .andExpect(model().attribute(
            "serviceRatings",
            DisplayableEnumOptionUtil.getDisplayableOptions(ServiceFeedbackRating.class)
        ));
  }

  @SecurityTest
  void submitApplicationFeedback_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(FeedbackController.class)
            .submitFeedback(null, null, null)))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void submitApplicationFeedback_assertRedirect() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION))
    ).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(FeedbackController.class)
            .submitApplicationFeedback(xyzApplication, null, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))
        ));

    verify(feedbackService).saveFeedback(eq(xyzApplication), any(), any(), eq(organisationUser));
  }

  @Test
  void submitApplicationFeedback_whenHasErrors_assertOk() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.ORGANISATION,
        Set.of(Role.EDIT_APPLICATION))
    ).thenReturn(true);

    doAnswer(invocation -> {
      var bindingResult = (BindingResult) invocation.getArgument(1);
      bindingResult.addError(new ObjectError("error", "error"));

      return invocation;
    }).when(feedbackFormValidator).validate(any(), any());

    mockMvc.perform(post(ReverseRouter.route(on(FeedbackController.class)
            .submitApplicationFeedback(xyzApplication, null, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().isOk());

    verify(feedbackService, never()).saveFeedback(any(), any(), any());
  }
}
