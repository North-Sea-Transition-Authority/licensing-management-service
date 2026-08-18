package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

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

import java.math.BigDecimal;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RemoveEquityChangeController.class)
@ActiveProfiles({"test", "enable-lms2"})
class RemoveEquityChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private EquityChangeService equityChangeService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this beneficial interest change?";
  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this beneficial interest change?";
  private static final String VIEW_NAME = "lms/licence/correction/change/removeEquityChange";
  private static final List<SetEquityRow> SET_EQUITY_ROWS = List.of(new SetEquityRow("Org Ltd", BigDecimal.TEN));
  private static final List<TransferEquityHoldingView> TRANSFER_EQUITY_ROWS =
      List.of(new TransferEquityHoldingView("From Org Ltd", "To Org Ltd", BigDecimal.TEN, null));

  private final String positionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION_ID, POSITION_ID, null));

  @Test
  void renderRemoveExecutedEquityChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .renderRemoveExecutedEquityChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderRemoveExecutedEquityChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .renderRemoveExecutedEquityChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderRemoveExecutedEquityChange_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();
    var position = positionWithId();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(equityChangeService.getExecutedEquityChangeContext(CHANGE_ID))
        .thenReturn(new EquityChangeContext(SET_EQUITY_ROWS, TRANSFER_EQUITY_ROWS));

    mockMvc.perform(get(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .renderRemoveExecutedEquityChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", REMOVE_PAGE_TITLE),
            model().attribute("primaryButtonText", "Remove beneficial interest change"),
            model().attribute("setEquityRows", SET_EQUITY_ROWS),
            model().attribute("transferEquityRows", TRANSFER_EQUITY_ROWS),
            model().attribute("cancelUrl", positionUrl)
        );
  }

  @Test
  void removeEquityChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .removeEquityChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void removeEquityChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .removeEquityChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(equityChangeService);
  }

  @Test
  void removeEquityChange_whenEligible() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = positionWithId();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .removeEquityChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Beneficial interest change removed")
                .build())
        );

    verify(equityChangeService).removeExistingEquityChange(position, correction, CHANGE_ID);
  }

  @Test
  void renderUndoEquityChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .renderUndoEquityChange(CORRECTION_ID, CHANGE_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderUndoEquityChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .renderUndoEquityChange(CORRECTION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderUndoEquityChange_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = positionWithId();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(position)
        .build();
    var setEquityRows = List.of(new SetEquityRow("Org Ltd", BigDecimal.TEN));

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);
    when(equityChangeService.getEquityChangeContext(correction, CHANGE_ID))
        .thenReturn(new EquityChangeContext(SET_EQUITY_ROWS, TRANSFER_EQUITY_ROWS));

    mockMvc.perform(get(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .renderUndoEquityChange(CORRECTION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", UNDO_PAGE_TITLE),
            model().attribute("primaryButtonText", "Undo beneficial interest change"),
            model().attribute("setEquityRows", SET_EQUITY_ROWS),
            model().attribute("transferEquityRows", TRANSFER_EQUITY_ROWS),
            model().attribute("cancelUrl", positionUrl)
        );
  }

  @Test
  void undoEquityChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .undoEquityChange(CORRECTION_ID, CHANGE_ID, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void undoEquityChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .undoEquityChange(CORRECTION_ID, CHANGE_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(equityChangeService);
  }

  @Test
  void undoEquityChange_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = positionWithId();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(position)
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveEquityChangeController.class)
            .undoEquityChange(CORRECTION_ID, CHANGE_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Beneficial interest change undone")
                .build())
        );

    verify(equityChangeService).undoEquityChange(correction, CHANGE_ID);
  }

  private LicencePosition positionWithId() {
    return LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    return correction;
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}