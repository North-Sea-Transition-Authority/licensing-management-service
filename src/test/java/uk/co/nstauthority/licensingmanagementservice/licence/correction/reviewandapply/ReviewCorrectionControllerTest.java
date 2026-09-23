package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBanner;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicencePositionValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ContextConfiguration(classes = ReviewCorrectionController.class)
@ActiveProfiles("test")
class ReviewCorrectionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private EnergyPortalUserService energyPortalUserService;

  @MockitoBean
  private CorrectedTimelineService correctedTimelineService;

  @MockitoBean
  private CorrectionReviewService correctionReviewService;

  @MockitoBean
  private LicencePositionValidationService licencePositionValidationService;

  @MockitoBean
  private CorrectionApplyService correctionApplyService;

  @MockitoBean
  private TabbedLicencePageService tabbedLicencePageService;

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final String PAGE_CAPTION = "Licence - P1234";
  private static final String PAGE_TITLE = "Do you want to apply this correction?";
  private static final String DEFAULT_TAB_URL = "/licences/1/default-tab";
  private static final long ALLOCATED_TO_WUA_ID = 123L;
  private static final String USER_LOOKUP_PURPOSE = "Get correction allocated to user details";

  private final LicenceCorrection correction = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LicenceTestUtil.builder().withLicenceReference("P1234").build())
      .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
      .build();

  private final CorrectedTimeline correctedTimeline = new CorrectedTimeline(
      correction, List.of(), List.of(), new ResolvedStates(new TreeMap<>(), Map.of()));

  @Test
  void renderReviewCorrection_whenNotLoggedIn_thenRedirectToLogin() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(CORRECTION_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderReviewCorrection_whenNotAllocatedToUser_thenForbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderReviewCorrection_whenCorrectionIsCancelled_forbidden() throws Exception {
    var cancelledCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.CANCELLED)
        .build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(cancelledCorrection));

    mockMvc.perform(get(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderReviewCorrection_whenTimelineIsInvalid_thenTheErrorsAreSummarised() throws Exception {
    var blockingError = new PositionValidationError(
        UUID.randomUUID(), "1 February 2026", null, null, "Beneficial interests must total 100%");

    givenValidUserAndCorrectionPageWithPositionsAndErrors(List.of(), List.of(blockingError));

    var modelAndView = mockMvc.perform(get(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("canApply", false)
        )
        .andReturn().getModelAndView();

    assertThat(modelAndView.getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(PositionValidationError.toErrorSummaryItems(List.of(blockingError)));
  }

  @Test
  void renderReviewCorrection_whenAllocatedToUser_thenRenderReviewPage() throws Exception {
    var allocatedToUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(ALLOCATED_TO_WUA_ID)
        .withForename("Jane")
        .withSurname("Doe")
        .buildJson();
    var positions = List.of(reviewPosition());

    givenValidUserAndCorrectionPageWithPositionsAndErrors(positions, List.of());

    mockMvc.perform(get(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/reviewandapply/reviewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correction", correction),
            model().attribute("allocatedToUser", allocatedToUser.displayName()),
            model().attribute("createdDate", DateUtil.formatLongDate(correction.getCreatedInstant())),
            model().attribute("positions", positions),
            model().attribute("canApply", true),
            model().attribute("errorSummaryItems", List.of()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(CORRECTION_ID, null)))
        );
  }

  @Test
  void renderReviewCorrection_whenTheCorrectionHasNoChanges_thenItCannotBeApplied() throws Exception {
    givenValidUserAndCorrectionPageWithPositionsAndErrors(List.of(), List.of());

    mockMvc.perform(get(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("positions", List.of()),
            model().attribute("canApply", false),
            model().attribute("errorSummaryItems", List.of())
        );
  }

  @Test
  void renderReviewCorrection_whenCorrectionIsComplete_thenTheAppliedChangesAreShownReadOnly() throws Exception {
    var completeCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LicenceTestUtil.builder().withLicenceReference("P1234").build())
        .withCorrectionReference("COR-1")
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();
    var appliedPosition = new ReviewPositionView(
        UUID.randomUUID(), "1 January 2026", "COR-1", CorrectionMarker.POSITION_ADDED, List.of());

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(completeCorrection));
    when(licenceService.getLicencePageCaption(completeCorrection.getLicence())).thenReturn(PAGE_CAPTION);
    when(tabbedLicencePageService.getDefaultTabUrl(completeCorrection.getLicence())).thenReturn(DEFAULT_TAB_URL);
    when(energyPortalUserService.getByWuaId(WebUserAccountId.from(ALLOCATED_TO_WUA_ID), USER_LOOKUP_PURPOSE))
        .thenReturn(EnergyPortalUserTestUtil.newBuilder()
            .withWebUserAccountId(ALLOCATED_TO_WUA_ID)
            .buildJson());
    when(correctionReviewService.getAppliedPositions(completeCorrection)).thenReturn(List.of(appliedPosition));

    mockMvc.perform(get(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/reviewandapply/reviewCorrection"),
            model().attribute("pageTitle", "Correction COR-1"),
            model().attribute("isCorrectionApplied", true),
            model().attribute("positions", List.of(appliedPosition)),
            model().attribute("canApply", false),
            model().attribute("backLinkUrl", DEFAULT_TAB_URL),
            content().string(not(containsString("Apply correction")))
        );

    verifyNoInteractions(correctedTimelineService, licencePositionValidationService);
  }

  @Test
  void processApplyCorrection_whenNotLoggedIn_redirectToLogin() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(ReviewCorrectionController.class)
            .processApplyCorrection(CORRECTION_ID, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void processApplyCorrection_whenNotAllocatedToUser_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(post(ReverseRouter.route(on(ReviewCorrectionController.class)
            .processApplyCorrection(CORRECTION_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verify(correctionApplyService, never()).applyCorrection(any());
  }

  @Test
  void processApplyCorrection_whenCorrectionIsAlreadyComplete_forbidden() throws Exception {
    var completeCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(completeCorrection));

    mockMvc.perform(post(ReverseRouter.route(on(ReviewCorrectionController.class)
            .processApplyCorrection(CORRECTION_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verify(correctionApplyService, never()).applyCorrection(any());
  }

  @Test
  void processApplyCorrection_whenCorrectionIsNotApplicable_thenTheReviewPageIsRenderedWithTheBlockingErrors()
      throws Exception {
    var blockingError = new PositionValidationError(
        UUID.randomUUID(), "1 February 2026", null, null, "Beneficial interests must total 100%");
    var positions = List.of(reviewPosition());

    givenValidUserAndCorrectionPageWithPositionsAndErrors(positions, List.of(blockingError));
    when(correctionApplyService.applyCorrection(correction)).thenReturn(List.of(blockingError));

    var modelAndView = mockMvc.perform(post(ReverseRouter.route(on(ReviewCorrectionController.class)
            .processApplyCorrection(CORRECTION_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/reviewandapply/reviewCorrection"),
            model().attribute("positions", positions),
            model().attribute("canApply", false)
        )
        .andReturn().getModelAndView();

    assertThat(modelAndView.getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(PositionValidationError.toErrorSummaryItems(List.of(blockingError)));
  }

  @Test
  void processApplyCorrection_whenApplied_thenRedirectToTheLicenceWithASuccessBanner() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(tabbedLicencePageService.getDefaultTabUrl(correction.getLicence())).thenReturn(DEFAULT_TAB_URL);

    mockMvc.perform(post(ReverseRouter.route(on(ReviewCorrectionController.class)
            .processApplyCorrection(CORRECTION_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(DEFAULT_TAB_URL),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Correction %s applied".formatted(correction.getCorrectionReference()))
                .build())
        );

    verify(correctionApplyService).applyCorrection(correction);
  }

  private ReviewPositionView reviewPosition() {
    return new ReviewPositionView(
        UUID.randomUUID(),
        "1 February 2026",
        "COR-1",
        CorrectionMarker.POSITION_CORRECTED,
        List.of(new ReviewChangeView(
            new AdministratorChangeView(
                "Withdrawing Org Ltd",
                "Joining Org Ltd",
                UUID.randomUUID().toString(),
                null,
                ChangeViewUrls.none()),
            null,
            1)));
  }

  private void givenValidUserAndCorrectionPageWithPositionsAndErrors(
      List<ReviewPositionView> positions,
      List<PositionValidationError> blockingErrors
  ) {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(correction.getLicence())).thenReturn(PAGE_CAPTION);
    when(energyPortalUserService.getByWuaId(WebUserAccountId.from(ALLOCATED_TO_WUA_ID), USER_LOOKUP_PURPOSE))
        .thenReturn(EnergyPortalUserTestUtil.newBuilder()
            .withWebUserAccountId(ALLOCATED_TO_WUA_ID)
            .withForename("Jane")
            .withSurname("Doe")
            .buildJson());
    when(correctedTimelineService.getCorrectedTimeline(correction)).thenReturn(correctedTimeline);
    when(correctionReviewService.getReviewPositions(correctedTimeline, blockingErrors)).thenReturn(positions);
    when(licencePositionValidationService.validate(
        correctedTimeline.positionsToApply(),
        correctedTimeline.resolvedStates(),
        correctedTimeline.isCarbonStorage()))
        .thenReturn(blockingErrors);
  }
}
