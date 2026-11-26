package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action;

import static org.mockito.Mockito.mock;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = ChangeActionController.class)
@ActiveProfiles("test")
class ChangeActionControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final String TEMPLATE_NAME = "lms/licence/correction/change/subarea/removeSubareaChange";
  private static final String BLOCK_NAME = "30/1";
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final SubareaOperation OPERATION = LicenceOperation.newSubAreaOperation()
      .withBlockFeatureId(UUID.randomUUID())
      .build();
  private static final LicencePositionChange LIVE_CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.fromString(CHANGE_ID))
      .withLicencePosition(POSITION)
      .withOperations(List.of(OPERATION))
      .build();
  private static final LicencePositionStagedPayload EXECUTED_POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder()
      .withLicenceCorrection(CORRECTION)
      .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
      .withTargetLicencePosition(POSITION)
      .build();
  private static final LicencePositionStagedPayload ADDED_POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder()
      .withLicenceCorrection(CORRECTION)
      .withChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION)
      .withTargetLicencePosition(null)
      .build();

  @MockitoBean
  private ChangeActionService changeActionService;

  private final ChangeActionHandler handler = mock(ChangeActionHandler.class);

  private final String positionUrl = LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId());
  private final String addedPositionUrl =
      LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, ADDED_POSITION_STAGED_PAYLOAD.getPositionId());

  @Test
  void removeChange_whenNotLoggedIn_thenRedirectsToLogin() throws Exception {
    mockMvc.perform(get(removeUrl()))
        .andExpect(redirectionToLoginUrl());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
  void removeChange_whenCorrectionIsNotInProgress_thenForbidden(LicenceCorrectionStatus status) throws Exception {
    givenCorrectionAllocatedToUserWithStatus(status);

    mockMvc.perform(get(removeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void removeChange_whenPositionIsRemovedInCorrection_thenForbidden() throws Exception {
    givenCorrectionAllocatedToUser();
    givenLiveChange();
    when(licencePositionStagedPayloadService.isPositionRemovedInCorrection(CORRECTION, POSITION)).thenReturn(true);

    mockMvc.perform(get(removeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void removeChange_whenChangeHasNoVisibleOperation_thenNotFound() throws Exception {
    givenCorrectionAllocatedToUser();
    givenLiveChange();
    when(changeActionService.getOperationOfLiveChange(LIVE_CHANGE))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc.perform(get(removeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void removeChange_whenGet_thenShowsConfirmation() throws Exception {
    givenCorrectionAllocatedToUser();
    givenLiveChange();
    when(changeActionService.getOperationOfLiveChange(LIVE_CHANGE)).thenReturn(OPERATION);
    when(changeActionService.getHandler(OPERATION)).thenReturn(handler);
    when(handler.executedChangeConfirmation(CORRECTION, POSITION, LIVE_CHANGE))
        .thenReturn(new ModelAndView(TEMPLATE_NAME).addObject("blockName", BLOCK_NAME));

    mockMvc.perform(get(removeUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(TEMPLATE_NAME),
            model().attribute("pageTitle", "Are you sure you want to remove this subarea change?"),
            model().attribute("primaryButton", Button.warningButton("Remove subarea change")),
            model().attribute("blockName", BLOCK_NAME),
            model().attribute("cancelUrl", positionUrl)
        );

    verify(changeActionService, never()).removeLiveChange(CORRECTION, POSITION, LIVE_CHANGE);
  }

  @Test
  void removeChange_whenNotAllocatedToUser_thenForbidden() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(removeUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(changeActionService);
  }

  @Test
  void removeChange_whenPost_thenRemovesChangeAndRedirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenLiveChange();
    when(changeActionService.getOperationOfLiveChange(LIVE_CHANGE)).thenReturn(OPERATION);

    mockMvc.perform(post(removeUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Subarea change removed")
                .build())
        );

    verify(changeActionService).removeLiveChange(CORRECTION, POSITION, LIVE_CHANGE);
  }

  @Test
  void undoChange_whenNotLoggedIn_thenRedirectsToLogin() throws Exception {
    mockMvc.perform(get(undoUrl()))
        .andExpect(redirectionToLoginUrl());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
  void undoChange_whenCorrectionIsNotInProgress_thenForbidden(LicenceCorrectionStatus status) throws Exception {
    givenCorrectionAllocatedToUserWithStatus(status);

    mockMvc.perform(get(undoUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void undoChange_whenGetForChangeOnExecutedPosition_thenAsksToUndoAndCancelsBackToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenStagedChange(EXECUTED_POSITION_STAGED_PAYLOAD, ChangeAction.UNDO);
    when(changeActionService.getHandler(OPERATION)).thenReturn(handler);
    when(handler.stagedChangeConfirmation(CORRECTION, EXECUTED_POSITION_STAGED_PAYLOAD, CHANGE_ID, ChangeAction.UNDO))
        .thenReturn(new ModelAndView(TEMPLATE_NAME).addObject("blockName", BLOCK_NAME));

    mockMvc.perform(get(undoUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(TEMPLATE_NAME),
            model().attribute("pageTitle", "Are you sure you want to undo this subarea change?"),
            model().attribute("primaryButton", Button.warningButton("Undo subarea change")),
            model().attribute("blockName", BLOCK_NAME),
            model().attribute("cancelUrl", positionUrl)
        );
    verify(changeActionService, never()).undoStagedChange(EXECUTED_POSITION_STAGED_PAYLOAD, CHANGE_ID);
  }

  @Test
  void undoChange_whenGetForStagedRemoval_thenAsksToReinstate() throws Exception {
    givenCorrectionAllocatedToUser();
    givenStagedChange(EXECUTED_POSITION_STAGED_PAYLOAD, ChangeAction.REINSTATE);
    when(changeActionService.getHandler(OPERATION)).thenReturn(handler);
    when(handler.stagedChangeConfirmation(CORRECTION, EXECUTED_POSITION_STAGED_PAYLOAD, CHANGE_ID, ChangeAction.REINSTATE))
        .thenReturn(new ModelAndView(TEMPLATE_NAME).addObject("blockName", BLOCK_NAME));

    mockMvc.perform(get(undoUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(TEMPLATE_NAME),
            model().attribute("pageTitle", "Are you sure you want to reinstate this subarea change?"),
            model().attribute("primaryButton", Button.primaryButton("Reinstate subarea change")),
            model().attribute("blockName", BLOCK_NAME),
            model().attribute("cancelUrl", positionUrl)
        );
  }

  @Test
  void undoChange_whenGetForChangeOnAddedPosition_thenCancelsBackToAddedPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenStagedChange(ADDED_POSITION_STAGED_PAYLOAD, ChangeAction.UNDO);
    when(changeActionService.getHandler(OPERATION)).thenReturn(handler);
    when(handler.stagedChangeConfirmation(CORRECTION, ADDED_POSITION_STAGED_PAYLOAD, CHANGE_ID, ChangeAction.UNDO))
        .thenReturn(new ModelAndView(TEMPLATE_NAME).addObject("blockName", BLOCK_NAME));

    mockMvc.perform(get(undoUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("cancelUrl", addedPositionUrl)
        );
  }

  @Test
  void undoChange_whenNotAllocatedToUser_thenForbidden() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(undoUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(changeActionService);
  }

  @Test
  void undoChange_whenPostForChangeOnExecutedPosition_thenUndoesAndRedirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenStagedChange(EXECUTED_POSITION_STAGED_PAYLOAD, ChangeAction.UNDO);

    mockMvc.perform(post(undoUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Subarea change undone")
                .build())
        );

    verify(changeActionService).undoStagedChange(EXECUTED_POSITION_STAGED_PAYLOAD, CHANGE_ID);
  }

  @Test
  void undoChange_whenPostForStagedRemoval_thenReinstatesIt() throws Exception {
    givenCorrectionAllocatedToUser();
    givenStagedChange(EXECUTED_POSITION_STAGED_PAYLOAD, ChangeAction.REINSTATE);

    mockMvc.perform(post(undoUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Subarea change reinstated")
                .build())
        );

    verify(changeActionService).undoStagedChange(EXECUTED_POSITION_STAGED_PAYLOAD, CHANGE_ID);
  }

  @Test
  void undoChange_whenPostForChangeOnAddedPosition_thenRedirectsToAddedPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenStagedChange(ADDED_POSITION_STAGED_PAYLOAD, ChangeAction.UNDO);

    mockMvc.perform(post(undoUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(addedPositionUrl)
        );

    verify(changeActionService).undoStagedChange(ADDED_POSITION_STAGED_PAYLOAD, CHANGE_ID);
  }

  private String removeUrl() {
    return ReverseRouter.route(on(ChangeActionController.class).removeChange(CORRECTION, POSITION, LIVE_CHANGE, null, null));
  }

  private String undoUrl() {
    return ReverseRouter.route(on(ChangeActionController.class).undoChange(CORRECTION, CHANGE_ID, null, null));
  }

  private void givenLiveChange() {
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
  }

  private void givenStagedChange(LicencePositionStagedPayload positionStagedPayload, ChangeAction action) {
    when(licencePositionStagedPayloadService.getPositionStagedPayloadContainingChange(CORRECTION, CHANGE_ID))
        .thenReturn(positionStagedPayload);
    when(changeActionService.getOperationOfStagedChange(positionStagedPayload, CHANGE_ID)).thenReturn(OPERATION);
    when(changeActionService.getUndoAction(positionStagedPayload, CHANGE_ID)).thenReturn(action);
  }

  private void givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(CORRECTION));
  }

  private void givenCorrectionAllocatedToUserWithStatus(LicenceCorrectionStatus status) {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LICENCE)
        .withStatus(status)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}
