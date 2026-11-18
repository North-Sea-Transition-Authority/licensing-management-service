package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderReviewAndSubmitController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review.SubareaChangeReviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition.CorrectPositionChangeTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.partialsurrender.PartialSurrenderSummaryController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.subareachange.SubareaChangeSummaryController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

class ChangeViewUrlsFactoryTest {

  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID CHANGE_ID = UUID.randomUUID();

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build();
  private static final LicencePositionCorrection POSITION_CORRECTION =
      LicencePositionCorrectionTestUtil.newBuilder().build();
  private static final LicencePositionChange CHANGE_ENTITY =
      LicencePositionChangeTestUtil.newBuilder().withId(CHANGE_ID).build();

  private static final PositionChangeUrlContext EXECUTED_POSITION =
      PositionChangeUrlContext.forExecutedPosition(CORRECTION, LICENCE_POSITION, POSITION_CORRECTION);
  private static final PositionChangeUrlContext ADDED_POSITION =
      PositionChangeUrlContext.forAddedPosition(CORRECTION, POSITION_CORRECTION);

  private static final VisibleLicenceOperation ADMINISTRATOR =
      LicenceOperation.newAdministratorChange().withOperator(1).build();
  private static final VisibleLicenceOperation SET_EQUITY =
      LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(BigDecimal.TEN).build();
  private static final VisibleLicenceOperation TRANSFER_EQUITY = LicenceOperation.newTransferEquityOperation()
      .withTransferFrom(1)
      .withTransferTo(2)
      .withEquity(BigDecimal.TEN)
      .build();
  private static final VisibleLicenceOperation PARTIAL_SURRENDER = LicenceOperation.newPartialSurrenderOperation()
      .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
      .build();
  private static final UUID SUBAREA_BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final VisibleLicenceOperation SUBAREA =
      LicenceOperation.newSubAreaOperation().withBlockFeatureId(SUBAREA_BLOCK_FEATURE_ID).build();
  private static final VisibleLicenceOperation LICENSEE = LicenceOperation.newLicenseeOperation().build();
  private static final VisibleLicenceOperation STORAGE_OPERATOR =
      LicenceOperation.newStorageOperatorOperation().withOperator(1).build();
  private static final VisibleLicenceOperation EXPLORATION_OPERATOR =
      LicenceOperation.newExplorationOperatorOperation().withOperator(1).build();

  @Test
  void build_whenNoUrlContext_thenHasNoUrls() {
    var change = change(ADMINISTRATOR, LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS);

    var result = ChangeViewUrlsFactory.build(null, change, POSITION_ID, true, ADMINISTRATOR);

    assertThat(result).isEqualTo(ChangeViewUrls.none());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("actionUrlsByOperationAndChangeState")
  void build_whenChangeIsInAState_thenLinksToTheRoutesForItsOperation(
      String scenario,
      VisibleLicenceOperation operation,
      PositionChangeUrlContext urlContext,
      String changeType,
      String expectedCorrect,
      String expectedRemove,
      String expectedUndo,
      String expectedReinstate,
      String expectedView
  ) {
    var change = change(operation, changeType);

    var result = ChangeViewUrlsFactory.build(urlContext, change, POSITION_ID, false, operation);

    var expectedCorrectPosition = LicencePositionChangeType.REMOVE_CHANGE.equals(changeType)
        ? null
        : correctPositionUrl();
    assertThat(result).isEqualTo(new ChangeViewUrls(
        expectedCorrect,
        expectedRemove,
        expectedUndo,
        expectedReinstate,
        null,
        expectedCorrectPosition,
        expectedView
    ));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("viewUrlsOnTheTimeline")
  void build_whenTimelinePosition_thenOnlyHasTheViewUrl(
      String scenario,
      VisibleLicenceOperation operation,
      String expectedView
  ) {
    var change = change(operation, null);

    var result = ChangeViewUrlsFactory.build(
        PositionChangeUrlContext.forTimelinePosition(LICENCE_POSITION), change, POSITION_ID, true, operation);

    assertThat(result).isEqualTo(new ChangeViewUrls(null, null, null, null, null, null, expectedView));
  }

  private static Stream<Arguments> viewUrlsOnTheTimeline() {
    return Stream.of(
        Arguments.of("partial surrender", PARTIAL_SURRENDER, ReverseRouter.route(on(PartialSurrenderSummaryController.class)
            .renderPartialSurrenderSummary(LICENCE_POSITION.getLicence(), LICENCE_POSITION, CHANGE_ENTITY, null))),
        Arguments.of("subarea", SUBAREA, ReverseRouter.route(on(SubareaChangeSummaryController.class)
            .renderSubareaChangeSummary(LICENCE_POSITION.getLicence(), LICENCE_POSITION, CHANGE_ENTITY, null))),
        Arguments.of("administrator", ADMINISTRATOR, null)
    );
  }

  @Test
  void build_whenLiveChangeHasNoCorrectRoute_thenHasRemoveAndPositionUrls() {
    var change = change(SET_EQUITY, null);

    var result = ChangeViewUrlsFactory.build(EXECUTED_POSITION, change, POSITION_ID, true, SET_EQUITY);

    assertThat(result).isEqualTo(
        new ChangeViewUrls(null, removeUrl(), null, null, correctChangeOrderUrl(), correctPositionUrl(), null)
    );
  }

  @Test
  void build_whenPositionCanBeReordered_thenHasCorrectChangeOrderUrl() {
    var change = change(SUBAREA, LicencePositionChangeType.UPDATE_CHANGE_ORDER);

    var result = ChangeViewUrlsFactory.build(EXECUTED_POSITION, change, POSITION_ID, true, SUBAREA);

    assertThat(result.correctChangeOrder()).isEqualTo(correctChangeOrderUrl());
  }

  @Test
  void build_whenChangeIsRemoved_thenHasNoReorderOrMoveUrls() {
    var change = change(SUBAREA, LicencePositionChangeType.REMOVE_CHANGE);

    var result = ChangeViewUrlsFactory.build(EXECUTED_POSITION, change, POSITION_ID, true, SUBAREA);

    assertThat(result).isEqualTo(new ChangeViewUrls(null, null, null, undoUrl(), null, null, null));
  }

  @Test
  void build_whenChangeMovedAway_thenHasNoUndoUrl() {
    var change = change(ADMINISTRATOR, null).asMovedAway();

    var result = ChangeViewUrlsFactory.build(EXECUTED_POSITION, change, POSITION_ID, false, ADMINISTRATOR);

    assertThat(result).isEqualTo(ChangeViewUrls.none());
  }

  @Test
  void build_whenRemovedChangeMovedAway_thenHasNoReinstateUrl() {
    var change = change(ADMINISTRATOR, LicencePositionChangeType.REMOVE_CHANGE).asMovedAway();

    var result = ChangeViewUrlsFactory.build(EXECUTED_POSITION, change, POSITION_ID, false, ADMINISTRATOR);

    assertThat(result).isEqualTo(ChangeViewUrls.none());
  }

  private static Stream<Arguments> actionUrlsByOperationAndChangeState() {
    var live = (String) null;
    var added = LicencePositionChangeType.ADD_CHANGE;
    var operationsUpdated = LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS;
    var orderUpdated = LicencePositionChangeType.UPDATE_CHANGE_ORDER;
    var removed = LicencePositionChangeType.REMOVE_CHANGE;
    var remove = removeUrl();
    var undo = undoUrl();

    var adminAdded = ReverseRouter.route(on(ChangeFormController.class)
        .addToAddedPosition(CORRECTION, POSITION_CORRECTION, AddPositionChangeType.ADMINISTRATOR, null, null, null));
    var adminExecuted = ReverseRouter.route(on(ChangeFormController.class)
        .addToExecutedPosition(CORRECTION, LICENCE_POSITION, AddPositionChangeType.ADMINISTRATOR, null, null, null));
    var adminCorrecting = ReverseRouter.route(on(ChangeFormController.class)
        .correctLiveChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY, null, null, null));

    var setEquityAdded = ReverseRouter.route(on(LicencePositionSetEquityController.class)
        .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION));
    var setEquityExecuted = ReverseRouter.route(on(LicencePositionSetEquityController.class)
        .renderSummaryForExecutedPosition(CORRECTION, LICENCE_POSITION));
    var transferEquityAdded = ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION));
    var transferEquityExecuted = ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForExecutedPosition(CORRECTION, LICENCE_POSITION));

    var partialSurrenderStaged = ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION, POSITION_CORRECTION, null));
    var partialSurrenderCorrecting = ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY, null));
    var partialSurrenderReviewStaged = ReverseRouter.route(on(PartialSurrenderReviewAndSubmitController.class)
        .renderReviewAndSubmit(CORRECTION, POSITION_CORRECTION, null));
    var partialSurrenderReviewCorrecting = ReverseRouter.route(on(PartialSurrenderReviewAndSubmitController.class)
        .renderReviewAndSubmitForCorrectingChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY, null));

    var licenseeAdded = ReverseRouter.route(on(ChangeFormController.class)
        .addToAddedPosition(CORRECTION, POSITION_CORRECTION, AddPositionChangeType.LICENSEE, null, null, null));
    var licenseeExecuted = ReverseRouter.route(on(ChangeFormController.class)
        .addToExecutedPosition(CORRECTION, LICENCE_POSITION, AddPositionChangeType.LICENSEE, null, null, null));
    var licenseeCorrecting = ReverseRouter.route(on(ChangeFormController.class)
        .correctLiveChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY, null, null, null));

    var storageOperatorAdded = ReverseRouter.route(on(ChangeFormController.class)
        .addToAddedPosition(CORRECTION, POSITION_CORRECTION, AddPositionChangeType.STORAGE_OPERATOR, null, null, null));
    var storageOperatorExecuted = ReverseRouter.route(on(ChangeFormController.class)
        .addToExecutedPosition(CORRECTION, LICENCE_POSITION, AddPositionChangeType.STORAGE_OPERATOR, null, null, null));
    var storageOperatorCorrecting = ReverseRouter.route(on(ChangeFormController.class)
        .correctLiveChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY, null, null, null));

    var subareaCorrect = ReverseRouter.route(on(SubareaChangeReviewController.class)
        .renderStagedReview(CORRECTION, POSITION_CORRECTION, SUBAREA_BLOCK_FEATURE_ID, null));
    var subareaCorrecting = ReverseRouter.route(on(SubareaChangeReviewController.class)
        .renderLiveReview(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY, null));

    return Stream.of(
        Arguments.of("administrator, live", ADMINISTRATOR, EXECUTED_POSITION, live,
            adminCorrecting, remove, null, null, null),
        Arguments.of("administrator, added", ADMINISTRATOR, EXECUTED_POSITION, added,
            adminExecuted, null, undo, null, null),
        Arguments.of("administrator, operations updated", ADMINISTRATOR, EXECUTED_POSITION, operationsUpdated,
            adminCorrecting, null, undo, null, null),
        Arguments.of("administrator, order updated", ADMINISTRATOR, EXECUTED_POSITION, orderUpdated,
            adminCorrecting, null, undo, null, null),
        Arguments.of("administrator, removed", ADMINISTRATOR, EXECUTED_POSITION, removed,
            null, null, null, undo, null),
        Arguments.of("administrator, added position", ADMINISTRATOR, ADDED_POSITION, added,
            adminAdded, null, undo, null, null),

        Arguments.of("set equity, live", SET_EQUITY, EXECUTED_POSITION, live,
            null, remove, null, null, null),
        Arguments.of("set equity, added", SET_EQUITY, EXECUTED_POSITION, added,
            setEquityExecuted, null, undo, null, null),
        Arguments.of("set equity, operations updated", SET_EQUITY, EXECUTED_POSITION, operationsUpdated,
            setEquityExecuted, null, undo, null, null),
        Arguments.of("set equity, order updated", SET_EQUITY, EXECUTED_POSITION, orderUpdated,
            null, null, undo, null, null),
        Arguments.of("set equity, removed", SET_EQUITY, EXECUTED_POSITION, removed,
            null, null, null, undo, null),
        Arguments.of("set equity, added position", SET_EQUITY, ADDED_POSITION, added,
            setEquityAdded, null, undo, null, null),

        Arguments.of("transfer equity, live", TRANSFER_EQUITY, EXECUTED_POSITION, live,
            null, remove, null, null, null),
        Arguments.of("transfer equity, added", TRANSFER_EQUITY, EXECUTED_POSITION, added,
            transferEquityExecuted, null, undo, null, null),
        Arguments.of("transfer equity, operations updated", TRANSFER_EQUITY, EXECUTED_POSITION, operationsUpdated,
            transferEquityExecuted, null, undo, null, null),
        Arguments.of("transfer equity, order updated", TRANSFER_EQUITY, EXECUTED_POSITION, orderUpdated,
            null, null, undo, null, null),
        Arguments.of("transfer equity, removed", TRANSFER_EQUITY, EXECUTED_POSITION, removed,
            null, null, null, undo, null),
        Arguments.of("transfer equity, added position", TRANSFER_EQUITY, ADDED_POSITION, operationsUpdated,
            transferEquityAdded, null, undo, null, null),

        Arguments.of("partial surrender, live", PARTIAL_SURRENDER, EXECUTED_POSITION, live,
            partialSurrenderCorrecting, remove, null, null, partialSurrenderReviewCorrecting),
        Arguments.of("partial surrender, added", PARTIAL_SURRENDER, EXECUTED_POSITION, added,
            partialSurrenderStaged, null, undo, null, partialSurrenderReviewStaged),
        Arguments.of("partial surrender, operations updated", PARTIAL_SURRENDER, EXECUTED_POSITION, operationsUpdated,
            partialSurrenderStaged, null, undo, null, partialSurrenderReviewStaged),
        Arguments.of("partial surrender, order updated", PARTIAL_SURRENDER, EXECUTED_POSITION, orderUpdated,
            partialSurrenderCorrecting, null, undo, null, partialSurrenderReviewCorrecting),
        Arguments.of("partial surrender, removed", PARTIAL_SURRENDER, EXECUTED_POSITION, removed,
            null, null, null, undo, null),
        Arguments.of("partial surrender, added position", PARTIAL_SURRENDER, ADDED_POSITION, added,
            partialSurrenderStaged, null, undo, null, partialSurrenderReviewStaged),
        Arguments.of("partial surrender, added position, live", PARTIAL_SURRENDER, ADDED_POSITION, live,
            partialSurrenderStaged, null, null, null, partialSurrenderReviewStaged),

        Arguments.of("licensee, live", LICENSEE, EXECUTED_POSITION, live,
            licenseeCorrecting, remove, null, null, null),
        Arguments.of("licensee, added", LICENSEE, EXECUTED_POSITION, added,
            licenseeExecuted, null, undo, null, null),
        Arguments.of("licensee, operations updated", LICENSEE, EXECUTED_POSITION, operationsUpdated,
            licenseeCorrecting, null, undo, null, null),
        Arguments.of("licensee, order updated", LICENSEE, EXECUTED_POSITION, orderUpdated,
            licenseeCorrecting, null, undo, null, null),
        Arguments.of("licensee, removed", LICENSEE, EXECUTED_POSITION, removed,
            null, null, null, undo, null),
        Arguments.of("licensee, added position", LICENSEE, ADDED_POSITION, added,
            licenseeAdded, null, undo, null, null),

        Arguments.of("storage operator, live", STORAGE_OPERATOR, EXECUTED_POSITION, live,
            storageOperatorCorrecting, remove, null, null, null),
        Arguments.of("storage operator, added", STORAGE_OPERATOR, EXECUTED_POSITION, added,
            storageOperatorExecuted, null, undo, null, null),
        Arguments.of("storage operator, operations updated", STORAGE_OPERATOR, EXECUTED_POSITION, operationsUpdated,
            storageOperatorCorrecting, null, undo, null, null),
        Arguments.of("storage operator, order updated", STORAGE_OPERATOR, EXECUTED_POSITION, orderUpdated,
            storageOperatorCorrecting, null, undo, null, null),
        Arguments.of("storage operator, removed", STORAGE_OPERATOR, EXECUTED_POSITION, removed,
            null, null, null, undo, null),
        Arguments.of("storage operator, added position", STORAGE_OPERATOR, ADDED_POSITION, added,
            storageOperatorAdded, null, undo, null, null),

        Arguments.of("subarea, live", SUBAREA, EXECUTED_POSITION, live,
            subareaCorrecting, remove, null, null, subareaCorrecting),
        Arguments.of("subarea, added", SUBAREA, EXECUTED_POSITION, added,
            subareaCorrect, null, undo, null, subareaCorrect),
        Arguments.of("subarea, operations updated", SUBAREA, EXECUTED_POSITION, operationsUpdated,
            subareaCorrect, null, undo, null, subareaCorrect),
        Arguments.of("subarea, order updated", SUBAREA, EXECUTED_POSITION, orderUpdated,
            subareaCorrecting, null, undo, null, subareaCorrecting),
        Arguments.of("subarea, removed", SUBAREA, EXECUTED_POSITION, removed,
            null, null, null, undo, null),
        Arguments.of("subarea, added position", SUBAREA, ADDED_POSITION, added,
            subareaCorrect, null, undo, null, subareaCorrect),
        Arguments.of("subarea, moved to added position", SUBAREA, ADDED_POSITION, operationsUpdated,
            subareaCorrect, null, undo, null, subareaCorrect),
        Arguments.of("exploration operator, live", EXPLORATION_OPERATOR, EXECUTED_POSITION, live,
            null, null, null, null, null),
        Arguments.of("exploration operator, added", EXPLORATION_OPERATOR, EXECUTED_POSITION, added,
            null, null, null, null, null),
        Arguments.of("exploration operator, removed", EXPLORATION_OPERATOR, EXECUTED_POSITION, removed,
            null, null, null, null, null)
    );
  }

  private static PositionChange change(VisibleLicenceOperation operation, String changeType) {
    return new PositionChange(CHANGE_ID.toString(), 1, changeType, List.of(operation));
  }

  private static String removeUrl() {
    return ReverseRouter.route(on(ChangeActionController.class)
        .removeChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY, null, null));
  }

  private static String undoUrl() {
    return ReverseRouter.route(on(ChangeActionController.class)
        .undoChange(CORRECTION, CHANGE_ID.toString(), null, null));
  }

  private static String correctChangeOrderUrl() {
    return ReverseRouter.route(on(CorrectChangeOrderController.class)
        .renderCorrectChangeOrder(CORRECTION, POSITION_ID, CHANGE_ID));
  }

  private static String correctPositionUrl() {
    return ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
        .renderMoveChangeTypePosition(CORRECTION.getId(), POSITION_ID, CHANGE_ID, null));
  }
}
