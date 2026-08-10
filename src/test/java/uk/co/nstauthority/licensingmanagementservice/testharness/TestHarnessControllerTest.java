package uk.co.nstauthority.licensingmanagementservice.testharness;

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

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = TestHarnessController.class)
@ActiveProfiles({"test", "test-harness"})
class TestHarnessControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicencePositionTestHarnessFormValidator licencePositionTestHarnessFormValidator;

  @MockitoBean
  private TestHarnessService testHarnessService;

  @MockitoBean
  private LicencePositionFeatureTestHarnessFormValidator licencePositionFeatureTestHarnessFormValidator;

  @MockitoBean
  private LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService;

  private static final Integer LICENCE_ID = 1;
  private static final Integer SECONDARY_LICENCE_ID = 2;
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(LICENCE_ID).withLicenceReference("P1").build();
  private static final Licence SECONDARY_LICENCE = LicenceTestUtil.builder()
      .withId(SECONDARY_LICENCE_ID).withLicenceReference("P2").build();
  private static final LicencePositionFeatureSeedState SEED_STATE = new LicencePositionFeatureSeedState(3, false);

  @Test
  void renderTestHarness_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(TestHarnessController.class).renderTestHarness())))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderTestHarness() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(TestHarnessController.class).renderTestHarness()))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/testHarness/testHarness"),
            model().attribute("licencePositionTestHarnessUrl",
                ReverseRouter.route(on(TestHarnessController.class).renderGenerateLicencePosition())),
            model().attribute("licencePositionFeatureTestHarnessUrl",
                ReverseRouter.route(on(TestHarnessController.class).renderLinkLicencePositionFeatures()))
        );
  }

  @Test
  void renderGenerateLicencePosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(TestHarnessController.class).renderGenerateLicencePosition())))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderGenerateLicencePosition() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(TestHarnessController.class).renderGenerateLicencePosition()))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/testHarness/licencePosition"),
            model().attributeExists("form"),
            model().attributeExists("searchUrl"),
            model().attribute("preSelectedLicence", java.util.Collections.emptyMap()),
            model().attribute("preSelectedSecondaryLicence", java.util.Collections.emptyMap()),
            model().attribute("cancelUrl",
                ReverseRouter.route(on(TestHarnessController.class).renderTestHarness()))
        );
  }

  @Test
  void generateLicencePosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(TestHarnessController.class)
            .generateLicencePosition(null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void generateLicencePosition() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(LICENCE);
    when(licenceService.findLicenceByIdOrThrow(SECONDARY_LICENCE_ID)).thenReturn(SECONDARY_LICENCE);

    var form = new LicencePositionTestHarnessForm();
    form.getLicenceId().setInputValue(LICENCE_ID.toString());
    form.getSecondaryLicenceId().setInputValue(SECONDARY_LICENCE_ID.toString());

    mockMvc.perform(post(ReverseRouter.route(on(TestHarnessController.class)
            .generateLicencePosition(null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(TestHarnessController.class).renderTestHarness())),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence positions generated for licence P1 and secondary licence P2")
                .build())
        );

    verify(licencePositionTestHarnessFormValidator).hasErrors(eq(form), any(BindingResult.class));
    verify(testHarnessService).generateLicencePositions(LICENCE, SECONDARY_LICENCE);
  }

  @Test
  void generateLicencePosition_invalidForm() throws Exception {
    var form = new LicencePositionTestHarnessForm();

    when(licencePositionTestHarnessFormValidator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(TestHarnessController.class)
            .generateLicencePosition(null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is2xxSuccessful(),
            view().name("lms/testHarness/licencePosition"),
            model().attribute("form", form),
            model().attributeExists("searchUrl"),
            model().attribute("cancelUrl",
                ReverseRouter.route(on(TestHarnessController.class).renderTestHarness()))
        );

    verify(licencePositionTestHarnessFormValidator).hasErrors(eq(form), any(BindingResult.class));
    verifyNoInteractions(testHarnessService);
  }

  @Test
  void renderLinkLicencePositionFeatures_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(TestHarnessController.class).renderLinkLicencePositionFeatures())))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderLinkLicencePositionFeatures() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(TestHarnessController.class).renderLinkLicencePositionFeatures()))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/testHarness/licencePositionFeatures"),
            model().attributeExists("form"),
            model().attributeExists("searchUrl"),
            model().attribute("preSelectedLicence", java.util.Collections.emptyMap()),
            model().attribute("cancelUrl",
                ReverseRouter.route(on(TestHarnessController.class).renderTestHarness()))
        );
  }

  @Test
  void linkLicencePositionFeatures() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(LICENCE);
    when(licencePositionFeatureTestHarnessService.getSeedState(LICENCE)).thenReturn(SEED_STATE);
    when(licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE)).thenReturn(12);

    var form = new LicencePositionFeatureTestHarnessForm();
    form.getLicenceId().setInputValue(LICENCE_ID.toString());

    mockMvc.perform(post(ReverseRouter.route(on(TestHarnessController.class)
            .linkLicencePositionFeatures(null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(TestHarnessController.class).renderTestHarness())),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("12 features created and linked across 3 positions on licence P1")
                .build())
        );

    verify(licencePositionFeatureTestHarnessFormValidator).hasErrors(eq(form), any(BindingResult.class), eq(SEED_STATE));
  }

  @Test
  void linkLicencePositionFeatures_invalidForm() throws Exception {
    var form = new LicencePositionFeatureTestHarnessForm();

    when(licencePositionFeatureTestHarnessFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(null)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(TestHarnessController.class)
            .linkLicencePositionFeatures(null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is2xxSuccessful(),
            view().name("lms/testHarness/licencePositionFeatures"),
            model().attribute("form", form)
        );

    verify(licencePositionFeatureTestHarnessService, never()).createAndLinkFeatures(any());
  }
}