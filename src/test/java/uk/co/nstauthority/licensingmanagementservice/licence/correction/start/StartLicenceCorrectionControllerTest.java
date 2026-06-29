package uk.co.nstauthority.licensingmanagementservice.licence.correction.start;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = StartLicenceCorrectionController.class)
@ActiveProfiles({"test", "enable-lms2"})
class StartLicenceCorrectionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private StartLicenceCorrectionFormValidator startLicenceCorrectionFormValidator;

  private static final Integer LICENCE_ID = 1;
  private static final Licence LICENCE = LicenceTestUtil.builder().withId(LICENCE_ID).build();
  private static final String PAGE_TITLE = "Start a licence correction";
  private static final String PAGE_CAPTION = "licence - 1";

  @BeforeEach
  void setUp() {
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(LICENCE);
  }

  @Test
  void renderStartLicenceCorrection_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(StartLicenceCorrectionController.class).renderStartLicenceCorrection(LICENCE))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderStartLicenceCorrection() throws Exception {
    givenCanStartCorrection();
    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(PAGE_CAPTION);

    mockMvc.perform(get(ReverseRouter.route(on(StartLicenceCorrectionController.class).renderStartLicenceCorrection(LICENCE)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/startCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attributeExists("form"),
            model().attribute("backLinkUrl",
                ReverseRouter.route(on(LicenceOverviewController.class).renderLicenceOverview(LICENCE_ID, null, null, null)))
        );
  }

  @Test
  void renderStartLicenceCorrection_cannotStartCorrection() throws Exception {
    givenCannotStartCorrection();

    mockMvc.perform(get(ReverseRouter.route(on(StartLicenceCorrectionController.class).renderStartLicenceCorrection(LICENCE)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void startLicenceCorrection_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(
        ReverseRouter.route(on(StartLicenceCorrectionController.class)
            .startLicenceCorrection(LICENCE, null, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void startLicenceCorrection() throws Exception {
    givenCanStartCorrection();
    var expectedCorrectionId = UUID.randomUUID();
    var startCorrectionForm = new StartLicenceCorrectionForm();
    startCorrectionForm.getCorrectionReference().setInputValue("TEST-REF");
    startCorrectionForm.getReason().setInputValue("Test reason");

    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(expectedCorrectionId)
        .build();
    when(licenceCorrectionService.startCorrection(
        LICENCE,
        startCorrectionForm.getCorrectionReference().getInputValue(),
        startCorrectionForm.getReason().getInputValue(),
        regulatorUser
    )).thenReturn(correction);

    mockMvc.perform(post(
        ReverseRouter.route(on(StartLicenceCorrectionController.class)
            .startLicenceCorrection(LICENCE, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", startCorrectionForm))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(expectedCorrectionId, null))),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence correction started")
                .build())
        );

    verify(startLicenceCorrectionFormValidator).hasErrors(eq(startCorrectionForm), any(BindingResult.class));
    verify(licenceCorrectionService).startCorrection(
        LICENCE,
        startCorrectionForm.getCorrectionReference().getInputValue(),
        startCorrectionForm.getReason().getInputValue(),
        regulatorUser
    );
  }

  @Test
  void startLicenceCorrection_invalidForm() throws Exception {
    givenCanStartCorrection();

    var startCorrectionForm = new StartLicenceCorrectionForm();

    when(startLicenceCorrectionFormValidator.hasErrors(eq(startCorrectionForm), any(BindingResult.class))).thenReturn(true);
    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(PAGE_CAPTION);

    mockMvc.perform(post(
            ReverseRouter.route(on(StartLicenceCorrectionController.class)
                .startLicenceCorrection(LICENCE, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", startCorrectionForm))
        .andExpectAll(
            status().is2xxSuccessful(),
            view().name("lms/licence/correction/startCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("form", startCorrectionForm),
            model().attribute("backLinkUrl",
                ReverseRouter.route(on(LicenceOverviewController.class).renderLicenceOverview(LICENCE_ID, null, null, null)))
        );

    verify(startLicenceCorrectionFormValidator).hasErrors(eq(startCorrectionForm), any(BindingResult.class));
    verifyNoInteractions(licenceCorrectionService);
  }

  @Test
  void startLicenceCorrection_cannotStartCorrection() throws Exception {
    givenCannotStartCorrection();

    mockMvc.perform(post(
            ReverseRouter.route(on(StartLicenceCorrectionController.class)
                .startLicenceCorrection(LICENCE, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(startLicenceCorrectionFormValidator);
    verifyNoInteractions(licenceCorrectionService);
  }

  private void givenCanStartCorrection() {
    when(licenceActionService.getAvailableUserActionItems(LICENCE, regulatorUser))
        .thenReturn(List.of(LicenceActionItem.START_CORRECTION.toActionItemView(LICENCE)));
  }

  private void givenCannotStartCorrection() {
    when(licenceActionService.getAvailableUserActionItems(LICENCE, regulatorUser))
        .thenReturn(List.of());
  }
}
