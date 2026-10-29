package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RemoveSubareaChangeController.class)
@ActiveProfiles("test")
class RemoveSubareaChangeControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this subarea change?";
  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this subarea change?";
  private static final String REINSTATE_PAGE_TITLE = "Are you sure you want to reinstate this subarea change?";
  private static final String VIEW_NAME = "lms/licence/correction/change/subarea/removeSubareaChange";
  private static final String BLOCK_NAME = "30/1";
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionCorrection ADDED_POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
      .withTargetLicencePosition(null)
      .build();

  @MockitoBean
  private SubareaChangeService subareaChangeService;

  private final String positionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION, null));
  private final String addedPositionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION, ADDED_POSITION_CORRECTION, null));

  @Test
  void renderRemoveExecutedSubareaChange_whenNotLoggedIn() throws Exception {
    var positionChange = LicencePositionChangeTestUtil.newBuilder().withId(UUID.fromString(CHANGE_ID)).build();

    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderRemoveExecutedSubareaChange(CORRECTION, POSITION, positionChange))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderRemoveExecutedSubareaChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();
    var positionChange = LicencePositionChangeTestUtil.newBuilder().withId(UUID.fromString(CHANGE_ID)).build();

    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderRemoveExecutedSubareaChange(CORRECTION, POSITION, positionChange)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderRemoveExecutedSubareaChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var subareaOperation = LicenceOperation.newSubAreaOperation().withBlockFeatureId(UUID.randomUUID()).build();
    var positionChange = givenExecutedSubareaChange(subareaOperation);

    when(subareaChangeService.getLiveSubareaOperationOrThrow(positionChange)).thenReturn(subareaOperation);
    when(subareaChangeService.getBlockName(subareaOperation)).thenReturn(BLOCK_NAME);

    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderRemoveExecutedSubareaChange(correction, POSITION, positionChange)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", REMOVE_PAGE_TITLE),
            model().attribute("primaryButton", Button.warningButton("Remove subarea change")),
            model().attribute("blockName", BLOCK_NAME),
            model().attribute("cancelUrl", positionUrl)
        );
  }

  @Test
  void removeSubareaChange_whenNotLoggedIn() throws Exception {
    var positionChange = LicencePositionChangeTestUtil.newBuilder().withId(UUID.fromString(CHANGE_ID)).build();

    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .removeSubareaChange(CORRECTION, POSITION, positionChange, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void removeSubareaChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();
    var positionChange = LicencePositionChangeTestUtil.newBuilder().withId(UUID.fromString(CHANGE_ID)).build();

    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .removeSubareaChange(CORRECTION, POSITION, positionChange, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(subareaChangeService);
  }

  @Test
  void removeSubareaChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionChange = givenExecutedSubareaChange(
        LicenceOperation.newSubAreaOperation().withBlockFeatureId(UUID.randomUUID()).build()
    );

    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .removeSubareaChange(correction, POSITION, positionChange, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Subarea change removed")
                .build())
        );

    verify(licencePositionCorrectionService).stageRemovalOfExecutedChange(correction, POSITION, CHANGE_ID);
  }

  @Test
  void renderUndoSubareaChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderUndoSubareaChange(CORRECTION, CHANGE_ID))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderUndoSubareaChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderUndoSubareaChange(CORRECTION, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, mode = EnumSource.Mode.EXCLUDE, names = {"LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION"})
  void renderUndoSubareaChange_whenLicenceTypeIsNotAllowed_thenForbidden(LicenceType licenceType) throws Exception {
    var notAllowedLicence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    var notAllowedCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(notAllowedLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(notAllowedCorrection));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(notAllowedCorrection));

    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderUndoSubareaChange(CORRECTION, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
  void renderUndoSubareaChange_whenCorrectionIsNotInProgress_thenForbidden(LicenceCorrectionStatus status) throws Exception {
    givenCorrectionAllocatedToUserWithStatus(status);

    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderUndoSubareaChange(CORRECTION, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderUndoSubareaChange_whenChangeIsOnAnExecutedPosition_thenCancelsBackToThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = executedPositionCorrection();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);
    givenStagedSubareaChange(positionCorrection);

    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderUndoSubareaChange(correction, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", UNDO_PAGE_TITLE),
            model().attribute("primaryButton", Button.warningButton("Undo subarea change")),
            model().attribute("blockName", BLOCK_NAME),
            model().attribute("cancelUrl", positionUrl)
        );
  }

  @Test
  void renderUndoSubareaChange_whenChangeIsOnAnAddedPosition_thenCancelsBackToTheAddedPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(ADDED_POSITION_CORRECTION);
    givenStagedSubareaChange(ADDED_POSITION_CORRECTION);

    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderUndoSubareaChange(correction, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("cancelUrl", addedPositionUrl)
        );
  }

  @Test
  void renderUndoSubareaChange_whenChangeIsAStagedRemoval_thenAsksToReinstateIt() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = executedPositionCorrection();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.isStagedRemoval(positionCorrection, CHANGE_ID)).thenReturn(true);
    givenStagedSubareaChange(positionCorrection);

    mockMvc.perform(get(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .renderUndoSubareaChange(correction, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", REINSTATE_PAGE_TITLE),
            model().attribute("primaryButton", Button.primaryButton("Reinstate subarea change")),
            model().attribute("blockName", BLOCK_NAME),
            model().attribute("cancelUrl", positionUrl)
        );
  }

  @Test
  void undoSubareaChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .undoSubareaChange(CORRECTION, CHANGE_ID, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void undoSubareaChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .undoSubareaChange(CORRECTION, CHANGE_ID, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(subareaChangeService);
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, mode = EnumSource.Mode.EXCLUDE, names = {"LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION"})
  void undoSubareaChange_whenLicenceTypeIsNotAllowed_thenForbidden(LicenceType licenceType) throws Exception {
    var notAllowedLicence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    var notAllowedCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(notAllowedLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(notAllowedCorrection));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(notAllowedCorrection));

    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .undoSubareaChange(CORRECTION, CHANGE_ID, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(subareaChangeService);
  }

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
  void undoSubareaChange_whenCorrectionIsNotInProgress_thenForbidden(LicenceCorrectionStatus status) throws Exception {
    givenCorrectionAllocatedToUserWithStatus(status);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .undoSubareaChange(CORRECTION, CHANGE_ID, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(subareaChangeService);
  }

  @Test
  void undoSubareaChange_whenChangeIsOnAnExecutedPosition_thenRedirectsToThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = executedPositionCorrection();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .undoSubareaChange(correction, CHANGE_ID, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Subarea change undone")
                .build())
        );

    verify(subareaChangeService).undoSubareaChange(positionCorrection, CHANGE_ID);
  }

  @Test
  void undoSubareaChange_whenChangeIsOnAnAddedPosition_thenRedirectsToTheAddedPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(ADDED_POSITION_CORRECTION);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .undoSubareaChange(correction, CHANGE_ID, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(addedPositionUrl)
        );

    verify(subareaChangeService).undoSubareaChange(ADDED_POSITION_CORRECTION, CHANGE_ID);
  }

  @Test
  void undoSubareaChange_whenChangeIsAStagedRemoval_thenReinstatesIt() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = executedPositionCorrection();

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.isStagedRemoval(positionCorrection, CHANGE_ID)).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveSubareaChangeController.class)
            .undoSubareaChange(correction, CHANGE_ID, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Subarea change reinstated")
                .build())
        );

    verify(subareaChangeService).undoSubareaChange(positionCorrection, CHANGE_ID);
  }

  private LicencePositionChange givenExecutedSubareaChange(SubareaOperation subareaOperation) {
    var positionChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.fromString(CHANGE_ID))
        .withLicencePosition(POSITION)
        .withOperations(List.of(subareaOperation))
        .build();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(licencePositionChangeService.findById(positionChange.getId())).thenReturn(Optional.of(positionChange));

    return positionChange;
  }

  private void givenStagedSubareaChange(LicencePositionCorrection positionCorrection) {
    var subareaOperation = LicenceOperation.newSubAreaOperation().withBlockFeatureId(UUID.randomUUID()).build();

    when(subareaChangeService.getStagedSubareaOperationOrThrow(positionCorrection, CHANGE_ID))
        .thenReturn(subareaOperation);
    when(subareaChangeService.getBlockName(subareaOperation)).thenReturn(BLOCK_NAME);
  }

  private LicencePositionCorrection executedPositionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(POSITION)
        .build();
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(CORRECTION));
    return CORRECTION;
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
