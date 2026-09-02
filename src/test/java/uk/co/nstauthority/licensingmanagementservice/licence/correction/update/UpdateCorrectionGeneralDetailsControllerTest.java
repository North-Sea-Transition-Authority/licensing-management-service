package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBanner;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = UpdateCorrectionGeneralDetailsController.class)
@ActiveProfiles({"test", "enable-lms2"})
class UpdateCorrectionGeneralDetailsControllerTest extends AbstractControllerTest {

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final String CORRECTION_REFERENCE = "COR-1";
  private static final String REASON = "Typo in executed position";
  private static final String PAGE_TITLE = "Update correction details";
  private static final Map<String, String> ALLOCATABLE_USERS = Map.of(
      String.valueOf(REGULATOR_USER_WUA_ID), "Regulator User",
      "42", "Jane Doe"
  );

  @MockitoBean
  private UpdateCorrectionGeneralDetailsFormValidator updateCorrectionGeneralDetailsFormValidator;

  @MockitoBean
  private UpdateCorrectionGeneralDetailsService updateCorrectionGeneralDetailsService;

  private final LicenceCorrection correction = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withCorrectionReference(CORRECTION_REFERENCE)
      .withReason(REASON)
      .withAllocatedToWuaId(REGULATOR_USER_WUA_ID)
      .build();

  @Test
  void renderUpdateGeneralDetails_whenNotLoggedIn_redirectToLogin() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .renderUpdateGeneralDetails(CORRECTION_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderUpdateGeneralDetails_whenNotAllocatedToUser_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .renderUpdateGeneralDetails(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderUpdateGeneralDetails_whenCorrectionIsNotInProgress_forbidden() throws Exception {
    var completedCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(completedCorrection));

    mockMvc.perform(get(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .renderUpdateGeneralDetails(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderUpdateGeneralDetails_whenAllocatedToUser_assertModelAndView() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(updateCorrectionGeneralDetailsService.getAllocatableUsers(correction.getLicence()))
        .thenReturn(ALLOCATABLE_USERS);

    var result = mockMvc.perform(get(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .renderUpdateGeneralDetails(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/updateGeneralDetails"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("allocatableUsers", ALLOCATABLE_USERS),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(CORRECTION_ID, null)))
        )
        .andReturn();

    var form = (UpdateCorrectionGeneralDetailsForm) result.getModelAndView().getModel().get("form");

    assertThat(form)
        .extracting(
            f -> f.getCorrectionReference().getInputValue(),
            f -> f.getReason().getInputValue(),
            UpdateCorrectionGeneralDetailsForm::getAllocatedToWuaId
        )
        .containsExactly(CORRECTION_REFERENCE, REASON, String.valueOf(REGULATOR_USER_WUA_ID));
  }

  @Test
  void updateGeneralDetails_whenNotLoggedIn_redirectToLogin() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .updateGeneralDetails(CORRECTION_ID, null, null, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void updateGeneralDetails_whenNotAllocatedToUser_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(post(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .updateGeneralDetails(CORRECTION_ID, null, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(updateCorrectionGeneralDetailsFormValidator);
    verify(updateCorrectionGeneralDetailsService, never())
        .updateGeneralDetails(any(), any(), any(), anyLong());
  }

  @Test
  void updateGeneralDetails_whenStillAllocatedToInvokingUser_thenRedirectsToCorrection() throws Exception {
    var form = new UpdateCorrectionGeneralDetailsForm();
    form.getCorrectionReference().setInputValue("NEW-REF");
    form.getReason().setInputValue("Updated reason");
    form.setAllocatedToWuaId(String.valueOf(REGULATOR_USER_WUA_ID));

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(updateCorrectionGeneralDetailsService.getAllocatableUsers(correction.getLicence()))
        .thenReturn(ALLOCATABLE_USERS);
    when(updateCorrectionGeneralDetailsFormValidator
        .hasErrors(eq(form), any(BindingResult.class), eq(ALLOCATABLE_USERS)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .updateGeneralDetails(CORRECTION_ID, null, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(CORRECTION_ID, null))),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence correction details updated")
                .build())
        );

    verify(updateCorrectionGeneralDetailsService).updateGeneralDetails(
        correction, "NEW-REF", "Updated reason", REGULATOR_USER_WUA_ID);
  }

  @Test
  void updateGeneralDetails_whenReallocatedToAnotherUser_thenRedirectsToWorkArea() throws Exception {
    var form = new UpdateCorrectionGeneralDetailsForm();
    form.getCorrectionReference().setInputValue(CORRECTION_REFERENCE);
    form.getReason().setInputValue(REASON);
    form.setAllocatedToWuaId("42");

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(updateCorrectionGeneralDetailsService.getAllocatableUsers(correction.getLicence()))
        .thenReturn(ALLOCATABLE_USERS);
    when(updateCorrectionGeneralDetailsFormValidator
        .hasErrors(eq(form), any(BindingResult.class), eq(ALLOCATABLE_USERS)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .updateGeneralDetails(CORRECTION_ID, null, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)))
        );

    verify(updateCorrectionGeneralDetailsService).updateGeneralDetails(
        correction, CORRECTION_REFERENCE, REASON, 42L);
  }

  @Test
  void updateGeneralDetails_whenInvalid_returnsToForm() throws Exception {
    var form = new UpdateCorrectionGeneralDetailsForm();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(updateCorrectionGeneralDetailsService.getAllocatableUsers(correction.getLicence()))
        .thenReturn(ALLOCATABLE_USERS);
    when(updateCorrectionGeneralDetailsFormValidator
        .hasErrors(eq(form), any(BindingResult.class), eq(ALLOCATABLE_USERS)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .updateGeneralDetails(CORRECTION_ID, null, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is2xxSuccessful(),
            view().name("lms/licence/correction/updateGeneralDetails"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("form", form),
            model().attribute("allocatableUsers", ALLOCATABLE_USERS)
        );

    verify(updateCorrectionGeneralDetailsService, never())
        .updateGeneralDetails(any(), any(), any(), anyLong());
  }
}