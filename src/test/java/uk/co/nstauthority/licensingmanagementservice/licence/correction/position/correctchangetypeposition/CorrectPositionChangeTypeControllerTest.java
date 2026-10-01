package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = CorrectPositionChangeTypeController.class)
@ActiveProfiles("test")
class CorrectPositionChangeTypeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private CorrectPositionChangeTypeFormValidator correctPositionChangeTypeFormValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID CHANGE_ID = UUID.randomUUID();
  private static final UUID OTHER_POSITION_ID = UUID.randomUUID();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 1);
  private static final String PAGE_TITLE = "Update position of change type";
  private static final String VIEW_NAME = "lms/licence/correction/change/moveChangeTypePosition";

  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .withPositionDate(POSITION_DATE)
      .withPositionOrder(2)
      .build();

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();

  private static final List<OrderablePosition> MOVE_OPTIONS = List.of(
      new OrderablePosition(POSITION_ID, POSITION_DATE, 1, "REF-A", false),
      new OrderablePosition(OTHER_POSITION_ID, POSITION_DATE, 2, "REF-B", false));

  private static final Map<String, String> MOVE_OPTION_LABELS = Map.of(
      POSITION_ID.toString(), "REF-A - 1 June 2026",
      OTHER_POSITION_ID.toString(), "REF-B - 1 June 2026 (2)");

  private static final String FORMATTED_POSITION_DATE = "1 June 2026 (2)";

  private static final List<String> ALLOWED_MOVES =
      List.of(POSITION_ID.toString(), OTHER_POSITION_ID.toString());

  private final String correctionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderCorrection(CORRECTION_ID, null));

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
    givenMoveOptions();

    mockMvc.perform(get(ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
            .renderMoveChangeTypePosition(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("positionDate", FORMATTED_POSITION_DATE),
            model().attribute("positionReference", POSITION.getLicenceTransaction().getRegulatorReference()),
            model().attribute("changeTypePositionMoveOptions", MOVE_OPTION_LABELS),
            model().attribute("backLinkUrl", correctionUrl)
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
  void correctChangeTypePosition_whenValid() throws Exception {
    givenCorrectionAllocatedToUser();
    givenPosition();
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
                .withHeadingContent("Change type position updated")
                .build())
        );
  }

  @Test
  void correctChangeTypePosition_whenInvalid() throws Exception {
    givenCorrectionAllocatedToUser();
    givenPosition();
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
            model().attribute("positionDate", FORMATTED_POSITION_DATE),
            model().attribute("positionReference", POSITION.getLicenceTransaction().getRegulatorReference()),
            model().attribute("changeTypePositionMoveOptions", MOVE_OPTION_LABELS),
            model().attribute("backLinkUrl", correctionUrl)
        );
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
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
  }

  private void givenMoveOptions() {
    when(licencePositionCorrectionService.getOrderableDatePositions(CORRECTION)).thenReturn(MOVE_OPTIONS);
  }
}