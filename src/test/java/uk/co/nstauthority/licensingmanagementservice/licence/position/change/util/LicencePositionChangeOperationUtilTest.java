package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

class LicencePositionChangeOperationUtilTest {

  private static final String LIVE_CHANGE_ID = UUID.randomUUID().toString();
  private static final UUID FEATURE_ID = UUID.randomUUID();
  private static final AdministratorOperation ADMINISTRATOR_OPERATION =
      LicenceOperation.newAdministratorChange().withOperator(116).build();
  private static final SetEquityOperation SET_EQUITY_OPERATION =
      new SetEquityOperation(300, BigDecimal.valueOf(50));

  private static Stream<Arguments> operationTypes() {
    return Stream.of(
        Arguments.of(AdministratorOperation.class, ADMINISTRATOR_OPERATION),
        Arguments.of(PartialSurrenderOperation.class, partialSurrender(FEATURE_ID)));
  }

  @ParameterizedTest
  @MethodSource("operationTypes")
  void containsOperation_whenStagedAsAnAddChange_thenTrue(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    var change = AddChange.buildOperationsChange(List.of(operation), 1);

    assertThat(LicencePositionChangeOperationUtil.containsOperation(change, operationType)).isTrue();
  }

  @ParameterizedTest
  @MethodSource("operationTypes")
  void containsOperation_whenStagedAsAnUpdateChange_thenTrue(
      Class<? extends LicenceOperation> operationType,
      LicenceOperation operation
  ) {
    var change = UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, operation);

    assertThat(LicencePositionChangeOperationUtil.containsOperation(change, operationType)).isTrue();
  }

  @Test
  void containsOperation_whenChangeCarriesAnotherOperationType_thenFalse() {
    var change = AddChange.buildOperationsChange(List.of(SET_EQUITY_OPERATION), 1);

    assertThat(LicencePositionChangeOperationUtil.containsOperation(change, AdministratorOperation.class)).isFalse();
  }

  @Test
  void containsOperation_whenRemoveChange_thenFalse() {
    var change = LicencePositionChangeType.removeChange().withChangeId(LIVE_CHANGE_ID).build();

    assertThat(LicencePositionChangeOperationUtil.containsOperation(change, AdministratorOperation.class)).isFalse();
  }

  @Test
  void containsOperation_whenLiveChangeCarriesTheOperation_thenTrue() {
    var liveChange = liveChange(ADMINISTRATOR_OPERATION);

    assertThat(LicencePositionChangeOperationUtil.containsOperation(liveChange, AdministratorOperation.class)).isTrue();
  }

  @Test
  void findOperation_whenLiveChangeCarriesTheOperation_thenReturnsIt() {
    var surrender = partialSurrender(FEATURE_ID);
    var liveChange = liveChange(ADMINISTRATOR_OPERATION, surrender);

    assertThat(LicencePositionChangeOperationUtil.findOperation(liveChange, PartialSurrenderOperation.class))
        .contains(surrender);
  }

  @Test
  void findOperation_whenLiveChangeCarriesAnotherOperationType_thenEmpty() {
    var liveChange = liveChange(SET_EQUITY_OPERATION);

    assertThat(LicencePositionChangeOperationUtil.findOperation(liveChange, AdministratorOperation.class)).isEmpty();
  }

  @Test
  void findOperation_whenLiveChangeHasNoOperations_thenEmpty() {
    assertThat(LicencePositionChangeOperationUtil
        .findOperation(new LicencePositionChange(), AdministratorOperation.class)).isEmpty();
  }

  @Test
  void changeExists_whenACarryingChangeIsStaged_thenTrue() {
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(SET_EQUITY_OPERATION), 1),
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FEATURE_ID)));

    assertThat(LicencePositionChangeOperationUtil.changeExists(changes, PartialSurrenderOperation.class)).isTrue();
  }

  @Test
  void changeExists_whenNoCarryingChangeIsStaged_thenFalse() {
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(SET_EQUITY_OPERATION), 1));

    assertThat(LicencePositionChangeOperationUtil.changeExists(changes, PartialSurrenderOperation.class)).isFalse();
  }

  @Test
  void findChange_returnsTheChangeCarryingTheOperation() {
    var surrenderChange = UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FEATURE_ID));
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 1),
        surrenderChange);

    assertThat(LicencePositionChangeOperationUtil.findChange(changes, PartialSurrenderOperation.class))
        .contains(surrenderChange);
  }

  @Test
  void findChange_whenNoChangeCarriesTheOperation_thenEmpty() {
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 1));

    assertThat(LicencePositionChangeOperationUtil.findChange(changes, PartialSurrenderOperation.class)).isEmpty();
  }

  @Test
  void findOperations_findsOperationsStagedAsAddsAndAsCorrections() {
    var added = partialSurrender(FEATURE_ID);
    var corrected = partialSurrender(UUID.randomUUID());
    var changes = List.<LicencePositionChangeType>of(
        AddChange.buildOperationsChange(List.of(added), 1),
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, corrected),
        AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 2),
        LicencePositionChangeType.removeChange().withChangeId(UUID.randomUUID().toString()).build());

    assertThat(LicencePositionChangeOperationUtil.findOperations(changes, PartialSurrenderOperation.class))
        .containsExactly(added, corrected);
  }

  @Test
  void upsertUpdateChange_whenNothingStaged_thenAddsAnUpdateChangeKeyedOnTheLiveChange() {
    var existing = AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 1);
    var operation = partialSurrender(FEATURE_ID);

    var result = LicencePositionChangeOperationUtil.upsertUpdateChange(
        List.of(existing), PartialSurrenderOperation.class, LIVE_CHANGE_ID, operation);

    assertThat(result).containsExactly(
        existing,
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, operation));
  }

  @Test
  void upsertUpdateChange_whenAlreadyStaged_thenReplacesItRatherThanStackingASecond() {
    var staged = UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FEATURE_ID));
    var corrected = partialSurrender(UUID.randomUUID());

    var result = LicencePositionChangeOperationUtil.upsertUpdateChange(
        List.of(staged), PartialSurrenderOperation.class, LIVE_CHANGE_ID, corrected);

    assertThat(result).containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, corrected));
  }

  @Test
  void replaceOperation_whenStagedAsAnAddChange_thenKeepsItAnAddChangeWithTheSameOrder() {
    var addChange = AddChange.buildOperationsChange(List.of(partialSurrender(FEATURE_ID)), 4);
    var corrected = partialSurrender(UUID.randomUUID());

    var result = LicencePositionChangeOperationUtil
        .replaceOperation(List.of(addChange), PartialSurrenderOperation.class, corrected);

    assertThat(result)
        .singleElement()
        .usingRecursiveComparison()
        .ignoringFields("changeId")
        .isEqualTo(AddChange.buildOperationsChange(List.of(corrected), 4));
  }

  @Test
  void replaceOperation_whenStagedAsAnUpdateChange_thenKeepsItKeyedOnTheOriginalChange() {
    var updateChange = UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FEATURE_ID));
    var corrected = partialSurrender(UUID.randomUUID());

    var result = LicencePositionChangeOperationUtil
        .replaceOperation(List.of(updateChange), PartialSurrenderOperation.class, corrected);

    assertThat(result).containsExactly(UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, corrected));
  }

  @Test
  void replaceOperation_leavesChangesCarryingOtherOperationsAlone() {
    var administratorChange = AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 1);
    var removeChange = LicencePositionChangeType.removeChange().withChangeId(UUID.randomUUID().toString()).build();
    var changes = List.<LicencePositionChangeType>of(
        administratorChange,
        removeChange,
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FEATURE_ID)));
    var corrected = partialSurrender(UUID.randomUUID());

    var result = LicencePositionChangeOperationUtil
        .replaceOperation(changes, PartialSurrenderOperation.class, corrected);

    assertThat(result).containsExactly(
        administratorChange,
        removeChange,
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, corrected));
  }

  @Test
  void removeChangesOf_dropsOnlyTheChangesCarryingTheOperation() {
    var administratorChange = AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 1);
    var changes = List.<LicencePositionChangeType>of(
        administratorChange,
        UpdateChangeOperations.buildUpdateChange(LIVE_CHANGE_ID, partialSurrender(FEATURE_ID)));

    var result = LicencePositionChangeOperationUtil.removeChangesOf(changes, PartialSurrenderOperation.class);

    assertThat(result).containsExactly(administratorChange);
  }

  private static PartialSurrenderOperation partialSurrender(UUID featureId) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(featureId))
        .build();
  }

  private static LicencePositionChange liveChange(LicenceOperation... operations) {
    var change = new LicencePositionChange();
    change.setOperations(List.of(operations));
    return change;
  }
}
