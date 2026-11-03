package uk.co.nstauthority.licensingmanagementservice.licence.pearsresync;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.LicenceWritebackResult;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.LicenceWritebackService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = PearsLicenceResyncController.class)
@ActiveProfiles({"test", "enable-pears-licence-resync"})
@TestPropertySource(properties = {
    "pears.datasource.url=example",
    "pears.datasource.username=example",
    "pears.datasource.password=example"
})
class PearsLicenceResyncControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceWritebackService licenceWritebackService;

  @MockitoBean
  private TabbedLicencePageService tabbedLicencePageService;

  private static final Integer LICENCE_ID = 1;
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(LICENCE_ID)
      .withLicenceReference("P1")
      .build();
  private static final String PAGE_CAPTION = "licence - 1";
  private static final String DEFAULT_TAB_URL = "/licences/1/default-tab";

  @BeforeEach
  void setUp() {
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(LICENCE);
  }

  @Test
  void renderResyncConfirmation_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(PearsLicenceResyncController.class).renderResyncConfirmation(LICENCE))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderResyncConfirmation_whenCanResync_thenConfirmationShown() throws Exception {
    givenCanResync();
    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(PAGE_CAPTION);
    when(tabbedLicencePageService.getDefaultTabUrl(LICENCE)).thenReturn(DEFAULT_TAB_URL);

    mockMvc.perform(get(ReverseRouter.route(on(PearsLicenceResyncController.class).renderResyncConfirmation(LICENCE)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/pearsresync/resyncLicenceFromPears"),
            model().attribute("pageTitle", "Are you sure you want to resync P1 from PEARS?"),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("cancelUrl", DEFAULT_TAB_URL)
        );
  }

  @Test
  void renderResyncConfirmation_whenCannotResync_thenForbidden() throws Exception {
    givenCannotResync();

    mockMvc.perform(get(ReverseRouter.route(on(PearsLicenceResyncController.class).renderResyncConfirmation(LICENCE)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void resync_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(PearsLicenceResyncController.class).resync(LICENCE, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());

    verifyNoInteractions(licenceWritebackService);
  }

  @Test
  void resync_whenPositionsRebuilt_thenSuccessBannerAndRedirectToLicence() throws Exception {
    givenCanResync();
    when(tabbedLicencePageService.getDefaultTabUrl(LICENCE)).thenReturn(DEFAULT_TAB_URL);
    when(licenceWritebackService.resync(LICENCE)).thenReturn(new LicenceWritebackResult(
        "Saved 2 positions, 3 changes and 4 operations for licence P1",
        2,
        3,
        4,
        0
    ));

    mockMvc.perform(post(ReverseRouter.route(on(PearsLicenceResyncController.class).resync(LICENCE, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(DEFAULT_TAB_URL),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence resynced from PEARS")
                .withOtherContent("Saved 2 positions, 3 changes and 4 operations for licence P1")
                .build())
        );

    verify(licenceWritebackService).resync(LICENCE);
  }

  @Test
  void resync_whenPearsHoldsNoPositions_thenInfoBannerAndRedirectToLicence() throws Exception {
    givenCanResync();
    when(tabbedLicencePageService.getDefaultTabUrl(LICENCE)).thenReturn(DEFAULT_TAB_URL);
    when(licenceWritebackService.resync(LICENCE)).thenReturn(new LicenceWritebackResult(
        "No positions found in PEARS for licence P1",
        0,
        0,
        0,
        0
    ));

    mockMvc.perform(post(ReverseRouter.route(on(PearsLicenceResyncController.class).resync(LICENCE, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(DEFAULT_TAB_URL),
            notificationBanner(NotificationBanner.newInfoBanner()
                .withHeadingContent("Licence not resynced from PEARS")
                .withOtherContent("No positions found in PEARS for licence P1")
                .build())
        );
  }

  @Test
  void resync_whenCannotResync_thenForbidden() throws Exception {
    givenCannotResync();

    mockMvc.perform(post(ReverseRouter.route(on(PearsLicenceResyncController.class).resync(LICENCE, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(licenceWritebackService);
  }

  private void givenCanResync() {
    when(licenceActionService.getAvailableUserActionItems(LICENCE, regulatorUser))
        .thenReturn(List.of(LicenceActionItem.RESYNC_FROM_PEARS.toActionItemView(LICENCE)));
  }

  private void givenCannotResync() {
    when(licenceActionService.getAvailableUserActionItems(LICENCE, regulatorUser))
        .thenReturn(List.of());
  }
}
