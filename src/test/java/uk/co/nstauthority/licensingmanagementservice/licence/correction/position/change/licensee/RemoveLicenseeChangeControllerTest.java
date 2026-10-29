package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.UndoLicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RemoveLicenseeChangeController.class)
@ActiveProfiles("test")
class RemoveLicenseeChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenseeChangeService licenseeChangeService;

  private static final Licence LICENCE = LicenceTestUtil.builder().withId(1).build();
  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this licensee change?";
  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this licensee change?";
  private static final String REINSTATE_PAGE_TITLE = "Are you sure you want to reinstate this licensee change?";
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/removeLicenseeChange";
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private final String updatePositionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION, null));

  @Test
  void renderLicenseeChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .renderUndoLicenseeChange(CORRECTION, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void undoLicenseeChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .undoLicenseeChange(CORRECTION, CHANGE_ID, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderRemoveExecutedLicenseChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();
    var positionChange = LicencePositionChangeTestUtil.newBuilder().withId(UUID.fromString(CHANGE_ID)).build();

    mockMvc.perform(get(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .renderRemoveExecutedLicenseeChange(CORRECTION, POSITION, positionChange)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void removeLicenseeChange_whenNotLoggedIn() throws Exception {
    var positionChange = LicencePositionChangeTestUtil.newBuilder().withId(UUID.fromString(CHANGE_ID)).build();

    mockMvc.perform(post(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .removeLicenseeChange(CORRECTION, POSITION, positionChange, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderRemoveExecutedLicenseeChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var operation = LicenceOperation.newLicenseeOperation().build();
    var positionChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.fromString(CHANGE_ID))
        .withLicencePosition(position)
        .withOperations(List.of(operation))
        .build();

    var licenseeChangeContext = new LicenseeChangeContext(
        List.of(2),
        List.of(1),
        List.of(1),
        List.of(2),
        Map.of(1, "SHELL", 2, "BP")
    );

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionChangeService.findById(positionChange.getId())).thenReturn(Optional.of(positionChange));
    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(licenseeChangeContext);

    var expectedLicenseeChangeView = new LicenseeChangeView(
        licenseeChangeContext.currentWithdrawingLicenseeNames(),
        licenseeChangeContext.currentJoiningLicenseeNames(),
        "",
        ChangeViewUrls.none()
    );

    mockMvc.perform(get(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .renderRemoveExecutedLicenseeChange(correction, position, positionChange)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(CONFIRMATION_TEMPLATE),
            model().attribute("pageTitle", REMOVE_PAGE_TITLE),
            model().attribute("primaryButton", Button.warningButton("Remove licensee change")),
            model().attribute("correctedLicenseeChangeView", expectedLicenseeChangeView),
            model().attribute("resultingLicensees", licenseeChangeContext.previousLicenseeNames()),
            model().attribute("cancelUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(correction, position, null)))
        );
  }

  @Test
  void removeLicenseeChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var operation = LicenceOperation.newLicenseeOperation().build();
    var positionChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.fromString(CHANGE_ID))
        .withLicencePosition(position)
        .withOperations(List.of(operation))
        .build();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionChangeService.findById(positionChange.getId())).thenReturn(Optional.of(positionChange));

    var expectedUrl = ReverseRouter.route(on(LicenceCorrectionController.class).renderLicencePosition(correction, position, null));

    mockMvc.perform(post(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .removeLicenseeChange(correction, position, positionChange, null)))
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(expectedUrl)
        );

    verify(licenseeChangeService).removeExistingLicenseeChange(position, correction, CHANGE_ID);
  }

  @Test
  void renderUndoLicenseeChange_whenAllocatedToUser_updatePositionChangeType() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().withLicencePositionId(POSITION_ID.toString()).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(position)
        .withPayload(payload)
        .build();

    var undoChangeContext = new UndoLicenseeChangeContext(
        List.of("currentWithdraw1"),
        List.of("currentJoin1"),
        List.of("restoreWithdraw1"),
        List.of("restoreJoin1"),
        List.of("org1, restoreJoin1")
    );
    var restoredLicenseeChangeView = new LicenseeChangeView(
        undoChangeContext.restoredWithdrawingLicenseeNames(),
        undoChangeContext.restoredJoiningLicenseeNames(),
        "",
        ChangeViewUrls.none()
    );

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);
    when(licenseeChangeService.getUndoLicenseeChangeContext(correction, positionCorrection, POSITION_ID))
        .thenReturn(undoChangeContext);
    when(licenseeChangeService.getRestoredLicenseeChangeView(
        undoChangeContext.restoredWithdrawingLicenseeNames(),
        undoChangeContext.restoredJoiningLicenseeNames())
    )
        .thenReturn(restoredLicenseeChangeView);

    var expectedCurrentChangeView = new LicenseeChangeView(
        List.of("currentWithdraw1"),
        List.of("currentJoin1"),
        "",
        new ChangeViewUrls(null, null, null, null, null, null)
    );

    mockMvc.perform(get(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .renderUndoLicenseeChange(correction, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(CONFIRMATION_TEMPLATE),
            model().attribute("pageTitle", UNDO_PAGE_TITLE),
            model().attribute("primaryButton", Button.warningButton("Undo licensee change")),
            model().attribute("correctedLicenseeChangeView", expectedCurrentChangeView),
            model().attribute("restoredLicenseeChangeView", restoredLicenseeChangeView),
            model().attribute("resultingLicensees", undoChangeContext.resultingLicenseeNames()),
            model().attribute("cancelUrl", updatePositionUrl)
        );
  }

  @Test
  void renderUndoLicenseeChange_whenAllocatedToUser_addPositionChangeType() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withTargetLicencePosition(position)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var undoChangeContext = new UndoLicenseeChangeContext(
        List.of("currentWithdraw1"),
        List.of("currentJoin1"),
        List.of("restoreWithdraw1"),
        List.of("restoreJoin1"),
        List.of("org1, restoreJoin1")
    );

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);
    when(licenseeChangeService.getUndoLicenseeChangeContext(correction, positionCorrection, POSITION_ID))
        .thenReturn(undoChangeContext);
    when(licenseeChangeService.getRestoredLicenseeChangeView(
        undoChangeContext.restoredWithdrawingLicenseeNames(),
        undoChangeContext.restoredJoiningLicenseeNames())
    )
        .thenReturn(null);

    var cancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(CORRECTION, positionCorrection, null));

    var expectedCurrentChangeView = new LicenseeChangeView(
        List.of("currentWithdraw1"),
        List.of("currentJoin1"),
        "",
        new ChangeViewUrls(null, null, null, null, null, null)
    );

    mockMvc.perform(get(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .renderUndoLicenseeChange(correction, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(CONFIRMATION_TEMPLATE),
            model().attribute("pageTitle", UNDO_PAGE_TITLE),
            model().attribute("primaryButton", Button.warningButton("Undo licensee change")),
            model().attribute("correctedLicenseeChangeView", expectedCurrentChangeView),
            model().attributeDoesNotExist("restoredLicenseeChangeView"),
            model().attribute("resultingLicensees", undoChangeContext.resultingLicenseeNames()),
            model().attribute("cancelUrl", cancelUrl)
        );
  }

  @Test
  void renderUndoLicenseeChange_reinstateRemovedChange_summariesDisplayCorrectly() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withTargetLicencePosition(position)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var undoChangeContext = new UndoLicenseeChangeContext(
        List.of("currentWithdraw1"),
        List.of("currentJoin1"),
        List.of("restoreWithdraw1"),
        List.of("restoreJoin1"),
        List.of("org1, restoreJoin1")
    );

    var expectedCurrentChangeView = new LicenseeChangeView(
        List.of("currentWithdraw1"),
        List.of("currentJoin1"),
        "",
        new ChangeViewUrls(null, null, null, null, null, null)
    );

    when(licencePositionCorrectionService.isStagedRemoval(positionCorrection, CHANGE_ID)).thenReturn(true);
    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);
    when(licenseeChangeService.getUndoLicenseeChangeContext(correction, positionCorrection, POSITION_ID))
        .thenReturn(undoChangeContext);
    when(licenseeChangeService.getRestoredLicenseeChangeView(
        undoChangeContext.restoredWithdrawingLicenseeNames(),
        undoChangeContext.restoredJoiningLicenseeNames())
    )
        .thenReturn(expectedCurrentChangeView);

    var cancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(CORRECTION, positionCorrection, null));

    mockMvc.perform(get(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .renderUndoLicenseeChange(correction, CHANGE_ID)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(CONFIRMATION_TEMPLATE),
            model().attribute("pageTitle", REINSTATE_PAGE_TITLE),
            model().attribute("primaryButton", Button.primaryButton("Reinstate licensee change")),
            model().attributeDoesNotExist("correctedLicenseeChangeView"),
            model().attribute("restoredLicenseeChangeView", expectedCurrentChangeView),
            model().attribute("resultingLicensees", undoChangeContext.resultingLicenseeNames()),
            model().attribute("cancelUrl", cancelUrl)
        );
  }

  @Test
  void undoLicenseeChange_addPositionChangeType() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withTargetLicencePosition(position)
        .build();

    var expectedUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, positionCorrection, null));

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .undoLicenseeChange(correction, CHANGE_ID, null)))
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(expectedUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licensee change undone")
                .build())
        );

    verify(licenseeChangeService).undoLicenseeChange(positionCorrection, CHANGE_ID);
  }

  @Test
  void undoLicenseeChange_reinstateRemovedChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.ADD_POSITION)
        .withTargetLicencePosition(position)
        .build();

    var expectedUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, positionCorrection, null));

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.isStagedRemoval(positionCorrection, CHANGE_ID)).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .undoLicenseeChange(correction, CHANGE_ID, null)))
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(expectedUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licensee change reinstated")
                .build())
        );

    verify(licenseeChangeService).undoLicenseeChange(positionCorrection, CHANGE_ID);
  }

  @Test
  void undoLicenseeChange_updatePositionChangeType() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(position)
        .build();

    var expectedUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition(), null));

    when(licencePositionCorrectionService.getPositionCorrectionContainingChange(correction, CHANGE_ID))
        .thenReturn(positionCorrection);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveLicenseeChangeController.class)
            .undoLicenseeChange(correction, CHANGE_ID, null)))
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(expectedUrl)
        );

    verify(licenseeChangeService).undoLicenseeChange(positionCorrection, CHANGE_ID);
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    return correction;
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}