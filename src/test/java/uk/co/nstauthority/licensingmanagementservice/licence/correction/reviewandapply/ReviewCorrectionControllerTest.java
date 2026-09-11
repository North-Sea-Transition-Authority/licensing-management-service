package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ContextConfiguration(classes = ReviewCorrectionController.class)
@ActiveProfiles("test")
class ReviewCorrectionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private EnergyPortalUserService energyPortalUserService;

  @MockitoBean
  private CorrectionReviewService correctionReviewService;

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final String PAGE_CAPTION = "Licence - P1234";
  private static final String PAGE_TITLE = "Do you want to apply this correction?";
  private static final long ALLOCATED_TO_WUA_ID = 123L;
  private static final String USER_LOOKUP_PURPOSE = "Get correction allocated to user details";

  private final LicenceCorrection correction = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LicenceTestUtil.builder().withLicenceReference("P1234").build())
      .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
      .build();

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
  void renderReviewCorrection_whenCorrectionIsNotInProgress_thenForbidden() throws Exception {
    var completedCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(completedCorrection));

    mockMvc.perform(get(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderReviewCorrection_whenAllocatedToUser_thenRenderReviewPage() throws Exception {
    var allocatedToUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(ALLOCATED_TO_WUA_ID)
        .withForename("Jane")
        .withSurname("Doe")
        .buildJson();
    var positions = List.of(new ReviewPositionView(
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
            1))));

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(correction.getLicence())).thenReturn(PAGE_CAPTION);
    when(energyPortalUserService.getByWuaId(WebUserAccountId.from(ALLOCATED_TO_WUA_ID), USER_LOOKUP_PURPOSE))
        .thenReturn(allocatedToUser);
    when(correctionReviewService.getReviewPositions(correction)).thenReturn(positions);

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
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(CORRECTION_ID, null)))
        );
  }
}