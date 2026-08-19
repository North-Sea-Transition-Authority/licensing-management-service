package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = UpdateCorrectionGeneralDetailsController.class)
@ActiveProfiles({"test", "enable-lms2"})
class UpdateCorrectionGeneralDetailsControllerTest extends AbstractControllerTest {

  @MockitoBean
  private UpdateCorrectionGeneralDetailsFormValidator updateCorrectionGeneralDetailsFormValidator;

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final String CORRECTION_REFERENCE = "COR-1";
  private static final String REASON = "Typo in executed position";
  private static final String PAGE_TITLE = "Update correction details";

  private final LicenceCorrection correction = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withCorrectionReference(CORRECTION_REFERENCE)
      .withReason(REASON)
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
  void renderUpdateGeneralDetails_whenAllocatedToUser_assertModelAndView() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));

    var mvcResult = mockMvc.perform(get(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .renderUpdateGeneralDetails(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/updateGeneralDetails"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(CORRECTION_ID, null)))
        )
        .andReturn();

    var form = (UpdateCorrectionGeneralDetailsForm) mvcResult.getModelAndView().getModel().get("form");
    assertThat(form.getCorrectionReference().getInputValue()).isEqualTo(CORRECTION_REFERENCE);
    assertThat(form.getReason().getInputValue()).isEqualTo(REASON);
  }

  @Test
  void updateGeneralDetails_whenNotLoggedIn_redirectToLogin() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .updateGeneralDetails(CORRECTION_ID, null, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void updateGeneralDetails_whenNotAllocatedToUser_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(post(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .updateGeneralDetails(CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(updateCorrectionGeneralDetailsFormValidator);
    verify(licenceCorrectionService, never()).updateGeneralDetails(any(), any(), any());
  }

  @Test
  void updateGeneralDetails_whenValid_updatesAndRedirects() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));

    var form = new UpdateCorrectionGeneralDetailsForm();
    form.getCorrectionReference().setInputValue("NEW-REF");
    form.getReason().setInputValue("Updated reason");

    when(updateCorrectionGeneralDetailsFormValidator.hasErrors(eq(form), any(BindingResult.class)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .updateGeneralDetails(CORRECTION_ID, null, null, null, null)))
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

    verify(updateCorrectionGeneralDetailsFormValidator).hasErrors(eq(form), any(BindingResult.class));
    verify(licenceCorrectionService).updateGeneralDetails(correction, "NEW-REF", "Updated reason");
  }

  @Test
  void updateGeneralDetails_whenInvalid_returnsToForm() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));

    var form = new UpdateCorrectionGeneralDetailsForm();

    when(updateCorrectionGeneralDetailsFormValidator.hasErrors(eq(form), any(BindingResult.class)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
            .updateGeneralDetails(CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is2xxSuccessful(),
            view().name("lms/licence/correction/updateGeneralDetails"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("form", form),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(CORRECTION_ID, null)))
        );

    verify(updateCorrectionGeneralDetailsFormValidator).hasErrors(eq(form), any(BindingResult.class));
    verify(licenceCorrectionService, never()).updateGeneralDetails(any(), any(), any());
  }
}