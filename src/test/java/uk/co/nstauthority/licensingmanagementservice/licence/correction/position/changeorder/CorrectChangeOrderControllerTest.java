package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMove;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveDirection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionOrderView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = CorrectChangeOrderController.class)
@ActiveProfiles({"test", "enable-lms2"})
class CorrectChangeOrderControllerTest extends AbstractControllerTest {

  @MockitoBean
  private CorrectChangeOrderFormValidator correctChangeOrderFormValidator;

  @MockitoBean
  private CorrectChangeOrderService correctChangeOrderService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID CHANGE_ID = UUID.randomUUID();
  private static final UUID OTHER_CHANGE_ID = UUID.randomUUID();
  private static final UUID ADDED_CORRECTION_ID = UUID.randomUUID();
  private static final LicencePositionCorrection ADDED_POSITION_CORRECTION =
      LicencePositionCorrectionTestUtil.newBuilder().withId(ADDED_CORRECTION_ID).build();
  private static final String SINGLE_OUTCOME_TITLE = "Do you want REF-A to be moved after REF-B?";
  private static final String MOVE_AFTER_OTHER =
      new PositionMove(PositionMoveDirection.AFTER, OTHER_CHANGE_ID).toFormValue();
  private static final Set<String> ONLY_MOVE_AFTER_OTHER = Set.of(MOVE_AFTER_OTHER);
  private static final String VIEW_NAME = "lms/licence/correction/correctChangeOrder";

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID).withLicence(LICENCE).build();

  private final String executedPositionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION_ID, POSITION_ID, null));

  private final String addedPositionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION_ID, ADDED_CORRECTION_ID, null));

  @Test
  void renderCorrectChangeOrder_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .renderCorrectChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderCorrectChangeOrder_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(CHANGE_ID, "REF-A"), orderable(OTHER_CHANGE_ID, "REF-B")));

    mockMvc.perform(get(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .renderCorrectChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", SINGLE_OUTCOME_TITLE),
            model().attributeExists("form"),
            model().attribute("changeMoveOptions", moveAfterOtherOption()),
            model().attribute("currentChangeOrder", List.of(
                new PositionOrderView(2, "REF-B", false),
                new PositionOrderView(1, "REF-A", true))),
            model().attribute("singleOutcome", true),
            model().attribute("backLinkUrl", executedPositionUrl)
        );
  }

  @Test
  void renderCorrectChangeOrder_whenMoreThanTwoChanges_offersMultipleOptions() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(
            orderable(CHANGE_ID, "REF-A"),
            orderable(OTHER_CHANGE_ID, "REF-B"),
            orderable(UUID.randomUUID(), "REF-C")));

    mockMvc.perform(get(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .renderCorrectChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", "Correct the order of REF-A"),
            model().attribute("singleOutcome", false)
        );
  }

  @Test
  void renderCorrectChangeOrder_buildsBeforeAndAfterOptionsAndCurrentOrder() throws Exception {
    var middleId = UUID.randomUUID();
    var lastId = UUID.randomUUID();

    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(
            orderable(CHANGE_ID, "REF-MOVED"),
            orderable(middleId, "REF-MIDDLE"),
            orderable(lastId, "REF-LAST")));

    var expectedOptions = new LinkedHashMap<String, String>();
    expectedOptions.put(
        new PositionMove(PositionMoveDirection.BEFORE, lastId).toFormValue(), "Before REF-LAST");
    expectedOptions.put(
        new PositionMove(PositionMoveDirection.AFTER, lastId).toFormValue(), "After REF-LAST");

    mockMvc.perform(get(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .renderCorrectChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("pageTitle", "Correct the order of REF-MOVED"),
            model().attribute("changeMoveOptions", expectedOptions),
            model().attribute("currentChangeOrder", List.of(
                new PositionOrderView(3, "REF-LAST", false),
                new PositionOrderView(2, "REF-MIDDLE", false),
                new PositionOrderView(1, "REF-MOVED", true))),
            model().attribute("singleOutcome", false)
        );
  }

  @Test
  void renderCorrectChangeOrder_whenChangeNotOnPosition_redirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(OTHER_CHANGE_ID, "REF-B")));

    mockMvc.perform(get(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .renderCorrectChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedPositionUrl)
        );
  }

  @Test
  void renderCorrectChangeOrder_whenNoOtherChanges_redirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(CHANGE_ID, "REF-A")));

    mockMvc.perform(get(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .renderCorrectChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedPositionUrl)
        );
  }

  @Test
  void renderCorrectChangeOrder_whenAddedPosition_backLinkPointsToAddedPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(ADDED_POSITION_CORRECTION));
    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(CHANGE_ID, "REF-A"), orderable(OTHER_CHANGE_ID, "REF-B")));

    mockMvc.perform(get(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .renderCorrectChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", addedPositionUrl)
        );
  }

  @Test
  void renderCorrectChangeOrder_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .renderCorrectChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void correctChangeOrder_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .correctChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void correctChangeOrder_whenValid() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    var form = new CorrectChangeOrderForm();
    form.getChangeMove().setInputValue(MOVE_AFTER_OTHER);

    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(CHANGE_ID, "REF-A"), orderable(OTHER_CHANGE_ID, "REF-B")));
    when(correctChangeOrderFormValidator.hasErrors(eq(form), any(), eq(ONLY_MOVE_AFTER_OTHER)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .correctChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedPositionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Change order updated")
                .build())
        );

    verify(correctChangeOrderService)
        .correctChangeOrder(CORRECTION, POSITION_ID, CHANGE_ID, OTHER_CHANGE_ID, PositionMoveDirection.AFTER);
  }

  @Test
  void correctChangeOrder_whenValidOnAddedPosition_redirectsToAddedPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.of(ADDED_POSITION_CORRECTION));
    var form = new CorrectChangeOrderForm();
    form.getChangeMove().setInputValue(MOVE_AFTER_OTHER);

    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(CHANGE_ID, "REF-A"), orderable(OTHER_CHANGE_ID, "REF-B")));
    when(correctChangeOrderFormValidator.hasErrors(eq(form), any(), eq(ONLY_MOVE_AFTER_OTHER)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .correctChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(addedPositionUrl)
        );

    verify(correctChangeOrderService)
        .correctChangeOrder(CORRECTION, POSITION_ID, CHANGE_ID, OTHER_CHANGE_ID, PositionMoveDirection.AFTER);
  }

  @Test
  void correctChangeOrder_whenInvalid() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    var form = new CorrectChangeOrderForm();

    when(correctChangeOrderService.getOrderableChanges(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(CHANGE_ID, "REF-A"), orderable(OTHER_CHANGE_ID, "REF-B")));
    when(correctChangeOrderFormValidator.hasErrors(eq(form), any(), eq(ONLY_MOVE_AFTER_OTHER)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .correctChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", SINGLE_OUTCOME_TITLE),
            model().attribute("form", form),
            model().attribute("changeMoveOptions", moveAfterOtherOption()),
            model().attribute("currentChangeOrder", List.of(
                new PositionOrderView(2, "REF-B", false),
                new PositionOrderView(1, "REF-A", true))),
            model().attribute("backLinkUrl", executedPositionUrl)
        );

    verify(correctChangeOrderService, never())
        .correctChangeOrder(any(), any(), any(), any(), any());
  }

  @Test
  void correctChangeOrder_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(CorrectChangeOrderController.class)
            .correctChangeOrder(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(correctChangeOrderFormValidator);
    verifyNoInteractions(correctChangeOrderService);
  }

  private void givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }

  private void givenExecutedPosition() {
    when(licencePositionCorrectionService.findFirstAddedPositionCorrection(CORRECTION, POSITION_ID))
        .thenReturn(Optional.empty());
  }

  private static LinkedHashMap<String, String> moveAfterOtherOption() {
    var options = new LinkedHashMap<String, String>();
    options.put(MOVE_AFTER_OTHER, "After REF-B");
    return options;
  }

  private static OrderableChange orderable(UUID id, String reference) {
    return new OrderableChange(id, reference);
  }
}