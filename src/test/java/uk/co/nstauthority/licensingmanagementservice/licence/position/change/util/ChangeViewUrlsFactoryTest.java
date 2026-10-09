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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.RemoveAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity.RemoveEquityChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee.LicencePositionLicenseeChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.RemovePartialSurrenderChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
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
  private static final VisibleLicenceOperation SUBAREA =
      LicenceOperation.newSubAreaOperation().withBlockFeatureId(UUID.randomUUID()).build();
  private static final VisibleLicenceOperation LICENSEE = LicenceOperation.newLicenseeOperation().build();

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
      String expectedUndo
  ) {
    var change = change(operation, changeType);

    var result = ChangeViewUrlsFactory.build(urlContext, change, POSITION_ID, false, operation);

    var expectedCorrectPosition = LicencePositionChangeType.REMOVE_CHANGE.equals(changeType)
        ? null
        : correctPositionUrl();
    assertThat(result).isEqualTo(
        new ChangeViewUrls(expectedCorrect, expectedRemove, expectedUndo, null, expectedCorrectPosition)
    );
  }

  @Test
  void build_whenOperationHasNoActionRoutes_thenOnlyHasPositionUrls() {
    var change = change(SUBAREA, null);

    var result = ChangeViewUrlsFactory.build(EXECUTED_POSITION, change, POSITION_ID, true, SUBAREA);

    assertThat(result).isEqualTo(
        new ChangeViewUrls(null, null, null, correctChangeOrderUrl(), correctPositionUrl())
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

    assertThat(result).isEqualTo(ChangeViewUrls.none());
  }

  @Test
  void build_whenChangeMovedAway_thenHasNoUndoUrl() {
    var change = change(ADMINISTRATOR, null).asMovedAway();

    var result = ChangeViewUrlsFactory.build(EXECUTED_POSITION, change, POSITION_ID, false, ADMINISTRATOR);

    assertThat(result).isEqualTo(ChangeViewUrls.none());
  }

  private static Stream<Arguments> actionUrlsByOperationAndChangeState() {
    var live = (String) null;
    var added = LicencePositionChangeType.ADD_CHANGE;
    var operationsUpdated = LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS;
    var orderUpdated = LicencePositionChangeType.UPDATE_CHANGE_ORDER;
    var removed = LicencePositionChangeType.REMOVE_CHANGE;

    var adminAdded = ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
        .renderForAddedPosition(CORRECTION, POSITION_CORRECTION));
    var adminExecuted = ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
        .renderForExecutedPosition(CORRECTION, LICENCE_POSITION));
    var adminCorrecting = ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
        .renderForCorrectingChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY));
    var adminRemove = ReverseRouter.route(on(RemoveAdministratorChangeController.class)
        .renderRemoveExecutedAdminChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY));
    var adminUndo = ReverseRouter.route(on(RemoveAdministratorChangeController.class)
        .renderUndoAdminChange(CORRECTION, CHANGE_ID.toString()));

    var setEquityAdded = ReverseRouter.route(on(LicencePositionSetEquityController.class)
        .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION));
    var setEquityExecuted = ReverseRouter.route(on(LicencePositionSetEquityController.class)
        .renderSummaryForExecutedPosition(CORRECTION, LICENCE_POSITION));
    var transferEquityAdded = ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION));
    var transferEquityExecuted = ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForExecutedPosition(CORRECTION, LICENCE_POSITION));
    var equityRemove = ReverseRouter.route(on(RemoveEquityChangeController.class)
        .renderRemoveExecutedEquityChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY));
    var equityUndo = ReverseRouter.route(on(RemoveEquityChangeController.class)
        .renderUndoEquityChange(CORRECTION, CHANGE_ID.toString()));

    var partialSurrenderStaged = ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION, POSITION_CORRECTION, null));
    var partialSurrenderCorrecting = ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY, null));
    var partialSurrenderRemove = ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
        .renderRemoveExecutedPartialSurrender(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY));
    var partialSurrenderUndo = ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
        .renderUndoPartialSurrender(CORRECTION, CHANGE_ID.toString()));

    var licenseeAdded = ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
        .renderForAddedPosition(CORRECTION, POSITION_CORRECTION));
    var licenseeExecuted = ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
        .renderForExecutedPosition(CORRECTION, LICENCE_POSITION));
    var licenseeCorrecting = ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
        .renderForCorrectingChange(CORRECTION, LICENCE_POSITION, CHANGE_ENTITY));

    return Stream.of(
        Arguments.of("administrator, live", ADMINISTRATOR, EXECUTED_POSITION, live,
            adminCorrecting, adminRemove, null),
        Arguments.of("administrator, added", ADMINISTRATOR, EXECUTED_POSITION, added,
            adminExecuted, null, adminUndo),
        Arguments.of("administrator, operations updated", ADMINISTRATOR, EXECUTED_POSITION, operationsUpdated,
            adminCorrecting, null, adminUndo),
        Arguments.of("administrator, order updated", ADMINISTRATOR, EXECUTED_POSITION, orderUpdated,
            adminCorrecting, null, adminUndo),
        Arguments.of("administrator, removed", ADMINISTRATOR, EXECUTED_POSITION, removed,
            null, null, adminUndo),
        Arguments.of("administrator, added position", ADMINISTRATOR, ADDED_POSITION, added,
            adminAdded, null, adminUndo),

        Arguments.of("set equity, live", SET_EQUITY, EXECUTED_POSITION, live,
            null, equityRemove, null),
        Arguments.of("set equity, added", SET_EQUITY, EXECUTED_POSITION, added,
            setEquityExecuted, null, equityUndo),
        Arguments.of("set equity, operations updated", SET_EQUITY, EXECUTED_POSITION, operationsUpdated,
            setEquityExecuted, null, equityUndo),
        Arguments.of("set equity, order updated", SET_EQUITY, EXECUTED_POSITION, orderUpdated,
            null, null, equityUndo),
        Arguments.of("set equity, removed", SET_EQUITY, EXECUTED_POSITION, removed,
            null, null, equityUndo),
        Arguments.of("set equity, added position", SET_EQUITY, ADDED_POSITION, added,
            setEquityAdded, null, equityUndo),

        Arguments.of("transfer equity, live", TRANSFER_EQUITY, EXECUTED_POSITION, live,
            null, equityRemove, null),
        Arguments.of("transfer equity, added", TRANSFER_EQUITY, EXECUTED_POSITION, added,
            transferEquityExecuted, null, equityUndo),
        Arguments.of("transfer equity, operations updated", TRANSFER_EQUITY, EXECUTED_POSITION, operationsUpdated,
            transferEquityExecuted, null, equityUndo),
        Arguments.of("transfer equity, order updated", TRANSFER_EQUITY, EXECUTED_POSITION, orderUpdated,
            null, null, equityUndo),
        Arguments.of("transfer equity, removed", TRANSFER_EQUITY, EXECUTED_POSITION, removed,
            null, null, equityUndo),
        Arguments.of("transfer equity, added position", TRANSFER_EQUITY, ADDED_POSITION, operationsUpdated,
            transferEquityAdded, null, equityUndo),

        Arguments.of("partial surrender, live", PARTIAL_SURRENDER, EXECUTED_POSITION, live,
            partialSurrenderCorrecting, partialSurrenderRemove, null),
        Arguments.of("partial surrender, added", PARTIAL_SURRENDER, EXECUTED_POSITION, added,
            partialSurrenderStaged, null, partialSurrenderUndo),
        Arguments.of("partial surrender, operations updated", PARTIAL_SURRENDER, EXECUTED_POSITION, operationsUpdated,
            partialSurrenderStaged, null, partialSurrenderUndo),
        Arguments.of("partial surrender, order updated", PARTIAL_SURRENDER, EXECUTED_POSITION, orderUpdated,
            partialSurrenderCorrecting, null, partialSurrenderUndo),
        Arguments.of("partial surrender, removed", PARTIAL_SURRENDER, EXECUTED_POSITION, removed,
            null, null, partialSurrenderUndo),
        Arguments.of("partial surrender, added position", PARTIAL_SURRENDER, ADDED_POSITION, added,
            partialSurrenderStaged, null, partialSurrenderUndo),

        Arguments.of("licensee, live", LICENSEE, EXECUTED_POSITION, live,
            licenseeCorrecting, null, null),
        Arguments.of("licensee, added", LICENSEE, EXECUTED_POSITION, added,
            licenseeExecuted, null, null),
        Arguments.of("licensee, operations updated", LICENSEE, EXECUTED_POSITION, operationsUpdated,
            licenseeCorrecting, null, null),
        Arguments.of("licensee, order updated", LICENSEE, EXECUTED_POSITION, orderUpdated,
            licenseeCorrecting, null, null),
        Arguments.of("licensee, removed", LICENSEE, EXECUTED_POSITION, removed,
            null, null, null),
        Arguments.of("licensee, added position", LICENSEE, ADDED_POSITION, added,
            licenseeAdded, null, null)
    );
  }

  private static PositionChange change(VisibleLicenceOperation operation, String changeType) {
    return new PositionChange(CHANGE_ID.toString(), 1, changeType, List.of(operation));
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
