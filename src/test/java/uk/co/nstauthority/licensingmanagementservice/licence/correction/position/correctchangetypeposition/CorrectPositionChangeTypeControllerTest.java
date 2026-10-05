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
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBannerDoesNotExist;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectPositionOrderForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectPositionOrderFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.OrderablePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMove;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveDirection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionOrderView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.MoveChangeToDateResult;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.OrderableChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = CorrectPositionChangeTypeController.class)
@ActiveProfiles("test")
class CorrectPositionChangeTypeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private CorrectPositionChangeTypeFormValidator correctPositionChangeTypeFormValidator;

  @MockitoBean
  private CorrectChangeOrderService correctChangeOrderService;

  @MockitoBean
  private CorrectPositionOrderFormValidator correctPositionOrderFormValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID CHANGE_ID = UUID.randomUUID();
  private static final UUID OTHER_POSITION_ID = UUID.randomUUID();
  private static final UUID NEW_POSITION_ID = UUID.randomUUID();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 1);
  private static final String CHANGE_REFERENCE = "Subarea – Block A";
  private static final String PAGE_TITLE = "Update position of Subarea – Block A";
  private static final LocalDate OTHER_DATE = LocalDate.of(2026, Month.JUNE, 30);
  private static final String VIEW_NAME = "lms/licence/correction/change/moveChangeTypePosition";
  private static final String NEW_POSITION_ORDER_PAGE_TITLE = "Where should the new position on 30 June 2026 go?";
  private static final String NEW_POSITION_ORDER_VIEW_NAME =
      "lms/licence/correction/correctPositionCorrectionOrder";

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();

  private static final OrderablePosition CURRENT_POSITION =
      new OrderablePosition(POSITION_ID, POSITION_DATE, 2, "REF-A", false);

  private static final OrderablePosition OTHER_POSITION =
      new OrderablePosition(OTHER_POSITION_ID, POSITION_DATE.plusDays(1), 2, "REF-B", false);

  private static final OrderablePosition FIRST_ON_OTHER_DATE =
      new OrderablePosition(UUID.randomUUID(), OTHER_DATE, 1, "REF-C", false);

  private static final OrderablePosition SECOND_ON_OTHER_DATE =
      new OrderablePosition(UUID.randomUUID(), OTHER_DATE, 2, "REF-D", true);

  private static final Map<String, String> MOVE_OPTIONS =
      Map.of(OTHER_POSITION_ID.toString(), "REF-B - 2 June 2026 (2)");

  private static final Set<String> ALLOWED_MOVES = Set.of(OTHER_POSITION_ID.toString());

  private static final Map<String, String> INSERT_OPTIONS = Map.of(
      new PositionMove(PositionMoveDirection.BEFORE, FIRST_ON_OTHER_DATE.id()).toFormValue(), "Before REF-C",
      new PositionMove(PositionMoveDirection.BEFORE, SECOND_ON_OTHER_DATE.id()).toFormValue(), "Before REF-D",
      new PositionMove(PositionMoveDirection.AFTER, SECOND_ON_OTHER_DATE.id()).toFormValue(), "After REF-D");

  private static final List<PositionOrderView> CURRENT_ORDER_ON_OTHER_DATE = List.of(
      new PositionOrderView(2, "REF-D", false),
      new PositionOrderView(1, "REF-C", false));

  private static final NotificationBanner CHANGE_TYPE_POSITION_UPDATED_BANNER = NotificationBanner.newSuccessBanner()
      .withHeadingContent("Position of Subarea – Block A updated")
      .build();

  private final String correctionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderCorrection(CORRECTION));

  private final String executedPositionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, new LicencePosition(POSITION_ID)));

  private final String changeTypePageUrl = ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
      .renderMoveChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null));

  private final CorrectPositionChangeTypeForm otherDateForm = buildOtherDateForm();

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
    verify(correctChangeOrderService, never()).moveChangeToDate(any(), any(), any(), any(), any());
  }

  @Test
  void correctChangeTypePosition_whenOtherDateMovesToAnExistingPosition_thenRedirectsToItsChangeOrderWithABanner()
      throws Exception {
    givenValidOtherDateSubmission(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_EXISTING_POSITION, OTHER_POSITION_ID));

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .correctChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", otherDateForm))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(CorrectChangeOrderController.class)
                .renderCorrectChangeOrder(CORRECTION, OTHER_POSITION_ID, CHANGE_ID))),
            notificationBanner(CHANGE_TYPE_POSITION_UPDATED_BANNER)
        );
  }

  @Test
  void correctChangeTypePosition_whenOtherDateMovesToANewPosition_thenRedirectsToTheCorrectionWithABanner()
      throws Exception {
    givenValidOtherDateSubmission(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_NEW_POSITION, NEW_POSITION_ID));

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .correctChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", otherDateForm))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(correctionUrl),
            notificationBanner(CHANGE_TYPE_POSITION_UPDATED_BANNER)
        );
  }

  @Test
  void correctChangeTypePosition_whenOtherDateNeedsThePositionOrder_thenRedirectsToTheNewPositionOrderPageWithoutABanner()
      throws Exception {
    givenValidOtherDateSubmission(
        new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.NEEDS_POSITION_ORDER, null));

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .correctChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", otherDateForm))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(changeTypePageUrl + "/position-order?positionDate=2026-06-30"),
            notificationBannerDoesNotExist()
        );
  }

  @Test
  void renderNewPositionOrder_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(newPositionOrderUrl()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderNewPositionOrder_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(newPositionOrderUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderNewPositionOrder_whenPositionRemovedInCorrection() throws Exception {
    givenCorrectionAllocatedToUser();
    givenPositionRemovedInCorrection();

    mockMvc.perform(get(newPositionOrderUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderNewPositionOrder_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();
    givenChangeOnPosition();
    givenPositionsOnOtherDate();

    mockMvc.perform(get(newPositionOrderUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(NEW_POSITION_ORDER_VIEW_NAME),
            model().attribute("pageTitle", NEW_POSITION_ORDER_PAGE_TITLE),
            model().attribute("singleOutcome", false),
            model().attributeExists("form"),
            model().attribute("positionMoveOptions", INSERT_OPTIONS),
            model().attribute("currentPositionOrder", CURRENT_ORDER_ON_OTHER_DATE),
            model().attribute("backLinkUrl", changeTypePageUrl)
        );
  }

  @Test
  void renderNewPositionOrder_whenChangeNotOnPosition_redirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    givenChangeNotOnPosition();

    mockMvc.perform(get(newPositionOrderUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedPositionUrl)
        );
  }

  @Test
  void renderNewPositionOrder_whenNoPositionsOnTheDate_redirectsToTheChangeTypePage() throws Exception {
    givenCorrectionAllocatedToUser();
    givenChangeOnPosition();
    when(licencePositionCorrectionService.getOrderablePositionsOnDate(CORRECTION, OTHER_DATE)).thenReturn(List.of());

    mockMvc.perform(get(newPositionOrderUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(changeTypePageUrl)
        );
  }

  @Test
  void placeNewPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(placeNewPositionUrl())
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void placeNewPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(placeNewPositionUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(correctPositionOrderFormValidator);
  }

  @Test
  void placeNewPosition_whenPositionRemovedInCorrection() throws Exception {
    givenCorrectionAllocatedToUser();
    givenPositionRemovedInCorrection();

    mockMvc.perform(post(placeNewPositionUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(correctPositionOrderFormValidator);
  }

  @Test
  void placeNewPosition_whenInvalid_thenRendersThePageWithTheForm() throws Exception {
    givenCorrectionAllocatedToUser();
    givenChangeOnPosition();
    givenPositionsOnOtherDate();

    var form = new CorrectPositionOrderForm();

    when(correctPositionOrderFormValidator
        .hasErrors(eq(form), any(BindingResult.class), eq(INSERT_OPTIONS.keySet())))
        .thenReturn(true);

    mockMvc.perform(post(placeNewPositionUrl())
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(NEW_POSITION_ORDER_VIEW_NAME),
            model().attribute("pageTitle", NEW_POSITION_ORDER_PAGE_TITLE),
            model().attribute("singleOutcome", false),
            model().attribute("form", form),
            model().attribute("positionMoveOptions", INSERT_OPTIONS),
            model().attribute("currentPositionOrder", CURRENT_ORDER_ON_OTHER_DATE),
            model().attribute("backLinkUrl", changeTypePageUrl)
        );

    verify(correctChangeOrderService, never()).moveChangeToDate(any(), any(), any(), any(), any());
  }

  @Test
  void placeNewPosition_whenValid_thenMovesTheChangeToThePlacedNewPositionAndRedirectsToTheCorrection()
      throws Exception {
    givenCorrectionAllocatedToUser();
    givenChangeOnPosition();
    givenPositionsOnOtherDate();

    var placement = new PositionMove(PositionMoveDirection.BEFORE, SECOND_ON_OTHER_DATE.id());
    var form = new CorrectPositionOrderForm();
    form.getPositionMove().setInputValue(placement.toFormValue());

    when(correctPositionOrderFormValidator
        .hasErrors(eq(form), any(BindingResult.class), eq(INSERT_OPTIONS.keySet())))
        .thenReturn(false);
    when(correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_ID, OTHER_DATE, placement))
        .thenReturn(new MoveChangeToDateResult(MoveChangeToDateResult.Outcome.MOVED_TO_NEW_POSITION, NEW_POSITION_ID));

    mockMvc.perform(post(placeNewPositionUrl())
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(correctionUrl),
            notificationBanner(CHANGE_TYPE_POSITION_UPDATED_BANNER)
        );

    verify(correctChangeOrderService).moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_ID, OTHER_DATE, placement);
  }

  @Test
  void placeNewPosition_whenChangeNotOnPosition_redirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    givenChangeNotOnPosition();

    mockMvc.perform(post(placeNewPositionUrl())
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", new CorrectPositionOrderForm()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedPositionUrl)
        );

    verifyNoInteractions(correctPositionOrderFormValidator);
  }

  @Test
  void placeNewPosition_whenNoPositionsOnTheDate_redirectsToTheChangeTypePage() throws Exception {
    givenCorrectionAllocatedToUser();
    givenChangeOnPosition();
    when(licencePositionCorrectionService.getOrderablePositionsOnDate(CORRECTION, OTHER_DATE)).thenReturn(List.of());

    mockMvc.perform(post(placeNewPositionUrl())
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", new CorrectPositionOrderForm()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(changeTypePageUrl)
        );

    verifyNoInteractions(correctPositionOrderFormValidator);
    verify(correctChangeOrderService, never()).moveChangeToDate(any(), any(), any(), any(), any());
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
    verify(correctChangeOrderService, never()).moveChangeToDate(any(), any(), any(), any(), any());
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
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(CORRECTION));
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

  private void givenChangeNotOnPosition() {
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(new OrderableChange(UUID.randomUUID(), "REF-B")));
  }

  private void givenPositionRemovedInCorrection() {
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionCorrectionService.isPositionRemovedInCorrection(CORRECTION, position)).thenReturn(true);
  }

  private void givenPositionsOnOtherDate() {
    when(licencePositionCorrectionService.getOrderablePositionsOnDate(CORRECTION, OTHER_DATE))
        .thenReturn(List.of(FIRST_ON_OTHER_DATE, SECOND_ON_OTHER_DATE));
  }

  private void givenValidOtherDateSubmission(MoveChangeToDateResult result) {
    givenCorrectionAllocatedToUser();
    givenPosition();
    givenChangeOnPosition();
    givenMoveOptions();

    when(correctPositionChangeTypeFormValidator
        .hasErrors(eq(otherDateForm), any(BindingResult.class), eq(ALLOWED_MOVES)))
        .thenReturn(false);
    when(correctChangeOrderService.moveChangeToDate(CORRECTION, POSITION_ID, CHANGE_ID, OTHER_DATE, null))
        .thenReturn(result);
  }

  private static String newPositionOrderUrl() {
    return ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
        .renderNewPositionOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, OTHER_DATE, null));
  }

  private static String placeNewPositionUrl() {
    return ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
        .placeNewPosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, OTHER_DATE, null, null, null, null));
  }

  private static CorrectPositionChangeTypeForm buildOtherDateForm() {
    var form = new CorrectPositionChangeTypeForm();
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);
    form.getCorrectPositionDate().setDate(OTHER_DATE);
    return form;
  }
}