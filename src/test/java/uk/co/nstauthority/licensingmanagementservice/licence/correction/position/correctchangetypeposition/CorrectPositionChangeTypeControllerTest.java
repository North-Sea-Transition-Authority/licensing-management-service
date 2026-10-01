package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

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

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.OrderablePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.OrderableChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = CorrectPositionChangeTypeController.class)
@ActiveProfiles("test")
class CorrectPositionChangeTypeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private CorrectPositionChangeTypeFormValidator correctPositionChangeTypeFormValidator;

  @MockitoBean
  private CorrectChangeOrderService correctChangeOrderService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID CHANGE_ID = UUID.randomUUID();
  private static final UUID OTHER_POSITION_ID = UUID.randomUUID();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 1);
  private static final String CHANGE_REFERENCE = "Subarea – Block A";
  private static final String PAGE_TITLE = "Update position of Subarea – Block A";
  private static final String VIEW_NAME = "lms/licence/correction/change/moveChangeTypePosition";

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();

  private static final OrderablePosition CURRENT_POSITION =
      new OrderablePosition(POSITION_ID, POSITION_DATE, 2, "REF-A", false);

  private static final OrderablePosition OTHER_POSITION =
      new OrderablePosition(OTHER_POSITION_ID, POSITION_DATE.plusDays(1), 2, "REF-B", false);

  private static final Map<String, String> MOVE_OPTIONS =
      Map.of(OTHER_POSITION_ID.toString(), "REF-B - 2 June 2026 (2)");

  private static final List<String> ALLOWED_MOVES = List.of(OTHER_POSITION_ID.toString());

  private final String correctionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderCorrection(CORRECTION_ID, null));

  private final String executedPositionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION_ID, POSITION_ID, null));

  @Test
  void renderMoveChangeTypePosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .renderMoveChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderMoveChangeTypePosition_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();
    givenPosition();
    givenChangeOnPosition();
    givenMoveOptions();

    mockMvc.perform(get(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .renderMoveChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("positionDate", "1 June 2026 (2)"),
            model().attribute("positionReference", "REF-A"),
            model().attribute("changeTypePositionMoveOptions", MOVE_OPTIONS),
            model().attribute("backLinkUrl", correctionUrl)
        );
  }

  @Test
  void renderMoveChangeTypePosition_whenChangeNotOnPosition_redirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(new OrderableChange(UUID.randomUUID(), "REF-B")));

    mockMvc.perform(get(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .renderMoveChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedPositionUrl)
        );
  }

  @Test
  void renderMoveChangeTypePosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .renderMoveChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void correctChangeTypePosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .correctChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void correctChangeTypePosition_whenAPositionIsSelected_thenMovesTheChange() throws Exception {
    givenCorrectionAllocatedToUser();
    givenPosition();
    givenChangeOnPosition();
    givenMoveOptions();

    var form = new CorrectPositionChangeTypeForm();
    form.getChangeTypePositionMove().setInputValue(OTHER_POSITION_ID.toString());

    when(correctPositionChangeTypeFormValidator
        .hasErrors(eq(form), any(BindingResult.class), eq(ALLOWED_MOVES)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .correctChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(correctionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Position of Subarea – Block A updated")
                .build())
        );

    verify(correctChangeOrderService).moveChangeToPosition(CORRECTION, POSITION_ID, CHANGE_ID, OTHER_POSITION_ID);
  }

  @Test
  void correctChangeTypePosition_whenOtherDateIsSelected_thenMovesNothing() throws Exception {
    givenCorrectionAllocatedToUser();
    givenPosition();
    givenChangeOnPosition();
    givenMoveOptions();

    var form = new CorrectPositionChangeTypeForm();
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);

    when(correctPositionChangeTypeFormValidator
        .hasErrors(eq(form), any(BindingResult.class), eq(ALLOWED_MOVES)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .correctChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(correctionUrl)
        );

    verify(correctChangeOrderService, never()).moveChangeToPosition(any(), any(), any(), any());
  }

  @Test
  void correctChangeTypePosition_whenInvalid() throws Exception {
    givenCorrectionAllocatedToUser();
    givenPosition();
    givenChangeOnPosition();
    givenMoveOptions();

    var form = new CorrectPositionChangeTypeForm();

    when(correctPositionChangeTypeFormValidator
        .hasErrors(eq(form), any(BindingResult.class), eq(ALLOWED_MOVES)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .correctChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("form", form),
            model().attribute("positionDate", "1 June 2026 (2)"),
            model().attribute("positionReference", "REF-A"),
            model().attribute("changeTypePositionMoveOptions", MOVE_OPTIONS),
            model().attribute("backLinkUrl", correctionUrl)
        );

    verify(correctChangeOrderService, never()).moveChangeToPosition(any(), any(), any(), any());
  }

  @Test
  void correctChangeTypePosition_whenChangeNotOnPosition_redirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(new OrderableChange(UUID.randomUUID(), "REF-B")));

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .correctChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", new CorrectPositionChangeTypeForm()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedPositionUrl)
        );

    verifyNoInteractions(correctPositionChangeTypeFormValidator);
  }

  @Test
  void correctChangeTypePosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .correctChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(correctPositionChangeTypeFormValidator);
    verifyNoInteractions(licencePositionService);
  }

  private void givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }

  private void givenPosition() {
    when(licencePositionCorrectionService.getOrderableDatePosition(CORRECTION, POSITION_ID))
        .thenReturn(CURRENT_POSITION);
  }

  private void givenChangeOnPosition() {
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(
            new OrderableChange(UUID.randomUUID(), "Subarea – Block B"),
            new OrderableChange(CHANGE_ID, CHANGE_REFERENCE)
        ));
  }

  private void givenExecutedPosition() {
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.empty());
  }

  private void givenMoveOptions() {
    when(licencePositionCorrectionService.getOrderableDatePositionsExcluding(CORRECTION, POSITION_ID))
        .thenReturn(List.of(OTHER_POSITION));
  }
}