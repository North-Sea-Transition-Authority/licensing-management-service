package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

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
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@ContextConfiguration(classes = ReinstateLicencePositionCorrectionController.class)
@ActiveProfiles({"test", "enable-lms2"})
class ReinstateLicencePositionCorrectionControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 1);
  private static final String PAGE_TITLE = "Are you sure you want to reinstate this position?";
  private static final String VIEW_NAME = "lms/licence/correction/reinstatePosition";
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .withPositionDate(POSITION_DATE)
      .build();

  private final String cancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderCorrection(CORRECTION_ID, null));

  @Test
  void renderReinstatePosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
            .renderReinstatePosition(CORRECTION_ID, POSITION_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderReinstatePosition_whenEligible() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    givenPositionReinstatable(correction, true);

    mockMvc.perform(get(ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
            .renderReinstatePosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("positionDate", DateUtil.formatLongDate(POSITION_DATE)),
            model().attribute("cancelUrl", cancelUrl)
        );
  }

  @Test
  void renderReinstatePosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();
    var reasonNotAllocated = String.format("Licence correction %s is not assigned to wuaId 1", CORRECTION_ID);
    mockMvc.perform(get(ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
            .renderReinstatePosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isForbidden(),
            status().reason(reasonNotAllocated)
        );
  }

  @Test
  void renderReinstatePosition_whenPositionNotReinstatable() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    givenPositionReinstatable(correction, false);


    var reasonNotReinstatable = String.format("Licence position %s is not marked for deletion and cannot be reinstated", POSITION_ID);
    mockMvc.perform(get(ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
            .renderReinstatePosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isForbidden(),
            status().reason(reasonNotReinstatable)
        );
  }

  @Test
  void reinstatePosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
            .reinstatePosition(CORRECTION_ID, POSITION_ID, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void reinstatePosition_whenEligible() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = givenPositionReinstatable(correction, true);

    mockMvc.perform(post(ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
            .reinstatePosition(CORRECTION_ID, POSITION_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(cancelUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence correction position reinstated")
                .build())
        );

    verify(licencePositionCorrectionService).reinstateDeletedPositionCorrection(correction, position);
  }

  @Test
  void reinstatePosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    var reasonNotAllocated = String.format("Licence correction %s is not assigned to wuaId 1", CORRECTION_ID);
    mockMvc.perform(post(ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
            .reinstatePosition(CORRECTION_ID, POSITION_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().isForbidden(),
            status().reason(reasonNotAllocated)
        );

    verify(licencePositionCorrectionService, never()).reinstateDeletedPositionCorrection(any(), any());
  }

  @Test
  void reinstatePosition_whenPositionNotReinstatable() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    givenPositionReinstatable(correction, false);

    var reasonNotReinstatable = String.format("Licence position %s is not marked for deletion and cannot be reinstated", POSITION_ID);
    mockMvc.perform(post(ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
            .reinstatePosition(CORRECTION_ID, POSITION_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().isForbidden(),
            status().reason(reasonNotReinstatable)
        );

    verify(licencePositionCorrectionService, never()).reinstateDeletedPositionCorrection(any(), any());
  }

  private LicencePosition givenPositionReinstatable(LicenceCorrection correction, boolean reinstatable) {
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(licencePositionCorrectionService.canReinstateDeletedPositionCorrection(correction, POSITION))
        .thenReturn(reinstatable);
    return POSITION;
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    return CORRECTION;
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}