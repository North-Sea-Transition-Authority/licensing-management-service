package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

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

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@ContextConfiguration(classes = UndoLicencePositionCorrectionController.class)
@ActiveProfiles("test")
class UndoLicencePositionCorrectionControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 1);
  private static final String CORRECTION_REFERENCE = "CORR-123";
  private static final String PAGE_TITLE = "Are you sure you want to undo this position?";
  private static final String VIEW_NAME = "lms/licence/correction/undoPosition";
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withId(POSITION_CORRECTION_ID)
      .withLicenceCorrection(CORRECTION)
      .withPayload(LicencePositionPayload.newCreateLicencePositionPayload()
          .withEffectiveDate(POSITION_DATE)
          .withCorrectionReference(CORRECTION_REFERENCE)
          .build())
      .build();

  private final String cancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderCorrection(CORRECTION));

  @Test
  void renderUndoPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
            .renderUndoPosition(CORRECTION, POSITION_CORRECTION))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderUndoPosition_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));

    mockMvc.perform(get(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
            .renderUndoPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("positionDate", DateUtil.formatLongDate(POSITION_DATE)),
            model().attribute("correctionReference", CORRECTION_REFERENCE),
            model().attribute("cancelUrl", cancelUrl)
        );
  }

  @Test
  void renderUndoPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
            .renderUndoPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void undoPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
            .undoPosition(CORRECTION, POSITION_CORRECTION, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void undoPosition_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));

    mockMvc.perform(post(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
            .undoPosition(CORRECTION, POSITION_CORRECTION, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(cancelUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence correction position undone")
                .build())
        );

    verify(licencePositionCorrectionService).undoPositionCorrection(POSITION_CORRECTION);
  }

  @Test
  void undoPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
            .undoPosition(CORRECTION, POSITION_CORRECTION, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(licencePositionCorrectionService);
  }

  private void givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(CORRECTION));
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}