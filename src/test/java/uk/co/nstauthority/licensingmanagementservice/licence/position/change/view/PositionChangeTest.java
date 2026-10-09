package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;

class PositionChangeTest {

  private static final String MOVED_CHANGE_ID = UUID.randomUUID().toString();
  private static final LicenceOperation MOVED_OPERATION =
      LicenceOperation.newAdministratorChange().withOperator(200).build();

  @Test
  void fromLicencePositionChanges_liveChangeHasNoChangeType() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(100).build();
    var licencePositionChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withChangeOrder(3)
        .withOperations(List.of(operation))
        .build();

    var result = PositionChange.fromLicencePositionChanges(List.of(licencePositionChange));

    assertThat(result)
        .singleElement()
        .satisfies(positionChange -> {
          assertThat(positionChange.changeId()).isEqualTo(licencePositionChange.getId().toString());
          assertThat(positionChange.changeOrder()).isEqualTo(3);
          assertThat(positionChange.changeType()).isNull();
          assertThat(positionChange.operations()).containsExactly(operation);
        });
  }

  @Test
  void fromCorrectionChanges_carriesChangeTypeAndUnwrapsOperations() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(200).build();
    var addOperation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(operation.id())
        .withOperation(operation)
        .build();
    var addChange = LicencePositionChangeType.addChange()
        .withChangeId("change-1")
        .withChangeOrder(1)
        .withOperations(List.of(addOperation))
        .build();

    var result = PositionChange.fromCorrectionChanges(List.of(addChange));

    assertThat(result)
        .singleElement()
        .satisfies(positionChange -> {
          assertThat(positionChange.changeId()).isEqualTo("change-1");
          assertThat(positionChange.changeOrder()).isEqualTo(1);
          assertThat(positionChange.changeType()).isEqualTo(LicencePositionChangeType.ADD_CHANGE);
          assertThat(positionChange.operations()).containsExactly(operation);
        });
  }

  @Test
  void validate_whenOperationInvalid_attachesChangeIdToError() {
    var change = PositionChangeTestUtil.newBuilder()
        .withChangeId("change-1")
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(5).build()))
        .build();

    var errors = change.validate(PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withAdministratorId(5).build())
        .build());

    assertThat(errors).singleElement().satisfies(error -> {
      assertThat(error.changeId()).isEqualTo("change-1");
      assertThat(error.operationType()).isEqualTo(LicenceOperation.LICENCE_ADMINISTRATOR);
    });
  }

  @Test
  void validate_whenChangeTypeIsRemove_returnsEmptyWithoutValidatingOperations() {
    var change = PositionChangeTestUtil.newBuilder()
        .withChangeId("change-1")
        .withChangeType(LicencePositionChangeType.REMOVE_CHANGE)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(5).build()))
        .build();

    var errors = change.validate(PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withAdministratorId(5).build())
        .build());

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenOperationsValid_returnsEmpty() {
    var change = PositionChangeTestUtil.newBuilder()
        .withChangeId("change-1")
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(5).build()))
        .build();

    var errors = change.validate(PositionValidationContextTestUtil.newBuilder()
        .withPreviousState(PositionStateTestUtil.newBuilder().withAdministratorId(6).build())
        .build());

    assertThat(errors).isEmpty();
  }

  @Test
  void foldChanges_whenNoCorrections_returnsLiveChangesSortedByChangeOrder() {
    var firstOperation = LicenceOperation.newAdministratorChange().withOperator(100).build();
    var secondOperation = LicenceOperation.newAdministratorChange().withOperator(200).build();
    var firstChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withChangeOrder(1).withOperations(List.of(firstOperation)).build();
    var secondChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withChangeOrder(2).withOperations(List.of(secondOperation)).build();

    var result = PositionChange.foldChanges(List.of(secondChange, firstChange), List.of());

    assertThat(result).containsExactly(
        new PositionChange(firstChange.getId().toString(), 1, null, List.of(firstOperation)),
        new PositionChange(secondChange.getId().toString(), 2, null, List.of(secondOperation)));
  }

  @Test
  void foldChanges_whenCorrectionAddsChangeWithNoLiveMatch_includesAsAddChange() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(100).build();
    var addChange = LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(1)
        .withOperations(List.of(addOperation(operation)))
        .build();

    var result = PositionChange.foldChanges(List.of(), List.of(addChange));

    assertThat(result).containsExactly(
        new PositionChange(addChange.changeId(), 1, LicencePositionChangeType.ADD_CHANGE, List.of(operation)));
  }

  @Test
  void foldChanges_whenRemoveCorrectionTargetsLiveChange_marksRemovedKeepingOperations() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(100).build();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withChangeOrder(2).withOperations(List.of(operation)).build();
    var removeChange = LicencePositionChangeType.removeChange()
        .withChangeId(liveChange.getId().toString()).build();

    var result = PositionChange.foldChanges(List.of(liveChange), List.of(removeChange));

    assertThat(result).containsExactly(
        new PositionChange(
            liveChange.getId().toString(), 2, LicencePositionChangeType.REMOVE_CHANGE, List.of(operation)));
  }

  @Test
  void foldChanges_whenUpdateChangeOrderCorrectionTargetsLiveChange_appliesNewOrderKeepingTypeAndOperations() {
    var operation = LicenceOperation.newAdministratorChange().withOperator(100).build();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withChangeOrder(2).withOperations(List.of(operation)).build();
    var updateChangeOrder = LicencePositionChangeType.updateChangeOrder()
        .withChangeId(liveChange.getId().toString()).withChangeOrder(5).build();

    var result = PositionChange.foldChanges(List.of(liveChange), List.of(updateChangeOrder));

    assertThat(result).containsExactly(
        new PositionChange(liveChange.getId().toString(), 5, null, List.of(operation)));
  }

  @Test
  void foldChanges_whenUpdateOperationsCorrectionTargetsLiveChange_appliesCorrectionOperationsKeepingOrder() {
    var liveOperation = LicenceOperation.newAdministratorChange().withOperator(100).build();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withChangeOrder(2).withOperations(List.of(liveOperation)).build();
    var correctedOperation = LicenceOperation.newAdministratorChange().withOperator(300).build();
    var updateOperations = UpdateChangeOperations.buildUpdateChange(liveChange.getId().toString(), correctedOperation);

    var result = PositionChange.foldChanges(List.of(liveChange), List.of(updateOperations));

    assertThat(result).containsExactly(
        new PositionChange(
            liveChange.getId().toString(), 2, LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS, List.of(correctedOperation)));
  }

  @ParameterizedTest
  @MethodSource("stagedMovesInEitherOrder")
  void foldChanges_whenALiveChangeIsMovedOntoThePosition_thenFoldsItsOperationsAtTheStagedOrder(
      List<LicencePositionChangeType> stagedMove
  ) {
    var result = PositionChange.foldChanges(List.of(), stagedMove);

    assertThat(result).containsExactly(new PositionChange(
        MOVED_CHANGE_ID,
        4,
        LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS,
        List.of(MOVED_OPERATION)
    ));
  }

  private static Stream<List<LicencePositionChangeType>> stagedMovesInEitherOrder() {
    var operations = UpdateChangeOperations.buildUpdateChange(MOVED_CHANGE_ID, MOVED_OPERATION);
    var changeOrder = LicencePositionChangeType.updateChangeOrder()
        .withChangeId(MOVED_CHANGE_ID).withChangeOrder(4).build();

    return Stream.of(List.of(operations, changeOrder), List.of(changeOrder, operations));
  }

  @ParameterizedTest
  @MethodSource("hiddenOperationLists")
  void isHidden_whenEveryOperationIsHidden_thenTrue(List<LicenceOperation> operations) {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(operations)
        .build();

    var result = change.isHidden();

    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @MethodSource("operationListsWithAVisibleOperation")
  void isHidden_whenAnyOperationIsVisible_thenFalse(List<LicenceOperation> operations) {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(operations)
        .build();

    var result = change.isHidden();

    assertThat(result).isFalse();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {
      LicencePositionChangeType.ADD_CHANGE,
      LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS,
      LicencePositionChangeType.UPDATE_CHANGE_ORDER
  })
  void canBeReordered_whenNotRemovedAndHasAVisibleOperation_thenTrue(String changeType) {
    var change = PositionChangeTestUtil.newBuilder()
        .withChangeType(changeType)
        .withOperations(List.of(visibleOperation(), hiddenOperation()))
        .build();

    var result = change.canBeReordered();

    assertThat(result).isTrue();
  }

  @Test
  void canBeReordered_whenChangeIsRemoved_thenFalse() {
    var change = PositionChangeTestUtil.newBuilder()
        .withChangeType(LicencePositionChangeType.REMOVE_CHANGE)
        .withOperations(List.of(visibleOperation()))
        .build();

    var result = change.canBeReordered();

    assertThat(result).isFalse();
  }

  @Test
  void canBeReordered_whenChangeIsHidden_thenFalse() {
    var change = PositionChangeTestUtil.newBuilder()
        .withChangeType(LicencePositionChangeType.ADD_CHANGE)
        .withOperations(List.of(hiddenOperation()))
        .build();

    var result = change.canBeReordered();

    assertThat(result).isFalse();
  }

  @Test
  void canBeReordered_whenChangeIsMovedAway_thenFalse() {
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(visibleOperation()))
        .build()
        .asMovedAway();

    var result = change.canBeReordered();

    assertThat(result).isFalse();
  }

  private static Stream<List<LicenceOperation>> hiddenOperationLists() {
    return Stream.of(
        List.of(),
        List.of(hiddenOperation()),
        List.of(hiddenOperation(), LicenceOperation.newBlockEndOperation().build())
    );
  }

  private static Stream<List<LicenceOperation>> operationListsWithAVisibleOperation() {
    return Stream.of(
        List.of(visibleOperation()),
        List.of(hiddenOperation(), visibleOperation())
    );
  }

  private static LicenceOperation hiddenOperation() {
    return LicenceOperation.newBlockCreateOperation().build();
  }

  private static LicenceOperation visibleOperation() {
    return LicenceOperation.newAdministratorChange().withOperator(100).build();
  }

  private static LicencePositionChangeOperation addOperation(LicenceOperation operation) {
    return LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(operation.id())
        .withOperation(operation)
        .build();
  }
}
