package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionCorrectionOrderChangeController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicencePositionCorrectionOrderChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private CorrectPositionOrderFormValidator correctPositionOrderFormValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID OTHER_POSITION_ID = UUID.randomUUID();
  private static final LocalDate DATE = LocalDate.of(2026, Month.JUNE, 1);
  private static final String SINGLE_OUTCOME_TITLE = "Do you want position REF-A to be moved after REF-B?";
  private static final String VIEW_NAME = "lms/licence/correction/correctPositionCorrectionOrder";

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID).withLicence(LICENCE).build();

  private final String backLinkUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderCorrection(CORRECTION_ID, null));

  @Test
  void renderCorrectPositionOrder_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(CORRECTION_ID, POSITION_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderCorrectPositionOrder_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.getOrderableSameDatePositions(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(POSITION_ID, 1, "REF-A"), orderable(OTHER_POSITION_ID, 2, "REF-B")));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", SINGLE_OUTCOME_TITLE),
            model().attributeExists("form"),
            model().attributeExists("positionMoveOptions"),
            model().attributeExists("currentPositionOrder"),
            model().attribute("singleOutcome", true),
            model().attribute("backLinkUrl", backLinkUrl)
        );
  }

  @Test
  void renderCorrectPositionOrder_whenMoreThanTwoSameDatePositions_offersMultipleOptions() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.getOrderableSameDatePositions(CORRECTION, POSITION_ID))
        .thenReturn(List.of(
            orderable(POSITION_ID, 1, "REF-A"),
            orderable(OTHER_POSITION_ID, 2, "REF-B"),
            orderable(UUID.randomUUID(), 3, "REF-C")));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", "Correct the order of position REF-A"),
            model().attributeExists("positionMoveOptions"),
            model().attribute("singleOutcome", false)
        );
  }

  @Test
  void renderCorrectPositionOrder_buildsBeforeAndAfterOptionsAndCurrentOrder() throws Exception {
    var middleId = UUID.randomUUID();
    var lastId = UUID.randomUUID();

    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.getOrderableSameDatePositions(CORRECTION, POSITION_ID))
        .thenReturn(List.of(
            orderable(POSITION_ID, 1, "REF-MOVED"),
            orderable(middleId, 2, "REF-MIDDLE"),
            orderable(lastId, 3, "REF-LAST")));

    var expectedOptions = new LinkedHashMap<String, String>();
    expectedOptions.put(
        new PositionMove(PositionMoveDirection.BEFORE, lastId).toFormValue(), "Before REF-LAST");
    expectedOptions.put(
        new PositionMove(PositionMoveDirection.AFTER, lastId).toFormValue(), "After REF-LAST");

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("pageTitle", "Correct the order of position REF-MOVED"),
            model().attribute("positionMoveOptions", expectedOptions),
            model().attribute("currentPositionOrder", List.of(
                new PositionOrderView(3, "REF-LAST", false),
                new PositionOrderView(2, "REF-MIDDLE", false),
                new PositionOrderView(1, "REF-MOVED", true))),
            model().attribute("singleOutcome", false)
        );
  }

  @Test
  void renderCorrectPositionOrder_whenNoOtherSameDatePositions_redirectsToCorrection() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.getOrderableSameDatePositions(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(POSITION_ID, 1, "REF-A")));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(backLinkUrl)
        );
  }

  @Test
  void renderCorrectPositionOrder_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void correctPositionOrder_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .correctLicencePositionCorrectionOrder(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void correctPositionOrder_whenValid() throws Exception {
    givenCorrectionAllocatedToUser();
    var form = new CorrectPositionOrderForm();
    form.getPositionMove().setInputValue(
        new PositionMove(PositionMoveDirection.AFTER, OTHER_POSITION_ID).toFormValue());

    when(licencePositionCorrectionService.getOrderableSameDatePositions(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(POSITION_ID, 1, "REF-A"), orderable(OTHER_POSITION_ID, 2, "REF-B")));
    when(correctPositionOrderFormValidator.hasErrors(eq(form), any(), any())).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .correctLicencePositionCorrectionOrder(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(backLinkUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence position order updated")
                .build())
        );

    verify(licencePositionCorrectionService)
        .correctPositionOrder(CORRECTION, POSITION_ID, OTHER_POSITION_ID, PositionMoveDirection.AFTER);
  }

  @Test
  void correctPositionOrder_whenInvalid() throws Exception {
    givenCorrectionAllocatedToUser();
    var form = new CorrectPositionOrderForm();

    when(licencePositionCorrectionService.getOrderableSameDatePositions(CORRECTION, POSITION_ID))
        .thenReturn(List.of(orderable(POSITION_ID, 1, "REF-A"), orderable(OTHER_POSITION_ID, 2, "REF-B")));
    when(correctPositionOrderFormValidator.hasErrors(eq(form), any(), any())).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .correctLicencePositionCorrectionOrder(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", SINGLE_OUTCOME_TITLE),
            model().attribute("form", form),
            model().attributeExists("positionMoveOptions"),
            model().attributeExists("currentPositionOrder"),
            model().attribute("backLinkUrl", backLinkUrl)
        );

    verify(licencePositionCorrectionService, never())
        .correctPositionOrder(any(), any(), any(), any());
  }

  @Test
  void correctPositionOrder_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .correctLicencePositionCorrectionOrder(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(correctPositionOrderFormValidator);
    verifyNoInteractions(licencePositionCorrectionService);
  }

  private void givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }

  private static OrderablePosition orderable(UUID id, int order, String reference) {
    return new OrderablePosition(id, DATE, order, reference, false);
  }
}