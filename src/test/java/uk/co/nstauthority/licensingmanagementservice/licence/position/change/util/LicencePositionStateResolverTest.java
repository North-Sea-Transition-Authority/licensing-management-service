package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

class LicencePositionStateResolverTest {

  @Test
  void resolveStates_snapshotsAdministratorPerPosition() {
    var positionOne = LicencePositionTestUtil.newBuilder().build();
    var positionTwo = LicencePositionTestUtil.newBuilder().build();

    var chronologicalOne = ChronologicalPositionTestUtil.live(
        positionOne, LicenceOperation.newAdministratorChange().withOperator(1).build());
    var chronologicalTwo = ChronologicalPositionTestUtil.live(
        positionTwo, LicenceOperation.newAdministratorChange().withOperator(2).build());

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(List.of(chronologicalOne, chronologicalTwo));

    assertThat(result.get(positionOne.getId()).administratorId()).isEqualTo(1);
    assertThat(result.get(positionTwo.getId()).administratorId()).isEqualTo(2);
  }

  @Test
  void resolveStates_carriesAdministratorForwardWhenPositionHasNoChange() {
    var oldest = LicencePositionTestUtil.newBuilder().build();
    var middle = LicencePositionTestUtil.newBuilder().build();
    var current = LicencePositionTestUtil.newBuilder().build();
    var later = LicencePositionTestUtil.newBuilder().build();

    var oldestChronological = ChronologicalPositionTestUtil.live(
        oldest,
        LicenceOperation.newAdministratorChange().withOperator(1).build()
    );
    var middleChronological = ChronologicalPositionTestUtil.live(
        middle,
        LicenceOperation.newAdministratorChange().withOperator(2).build()
    );
    var currentChronological = ChronologicalPositionTestUtil.live(current);
    var laterChronological = ChronologicalPositionTestUtil.live(
        later,
        LicenceOperation.newAdministratorChange().withOperator(3).build()
    );

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(oldestChronological, middleChronological, currentChronological, laterChronological)
    );

    // current has no change of its own, so it carries the administrator forward from the middle position
    assertThat(result.get(current.getId()).administratorId()).isEqualTo(2);
  }

  @Test
  void resolveStates_whenRemoveChange_skipsItAndCarriesPreviousAdministratorForward() {
    var earlier = LicencePositionTestUtil.newBuilder().build();
    var current = LicencePositionTestUtil.newBuilder().build();
    var later = LicencePositionTestUtil.newBuilder().build();

    var earlierChronological = ChronologicalPositionTestUtil.live(
        earlier, LicenceOperation.newAdministratorChange().withOperator(1).build());

    var removeChange = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        LicencePositionChangeType.REMOVE_CHANGE,
        List.of(LicenceOperation.newAdministratorChange().withOperator(2).build())
    );
    var currentChronological = ChronologicalPosition.fromLicencePosition(
        current, current.getPositionDate(), current.getPositionDateOrder(), List.of(removeChange));

    var laterChronological = ChronologicalPositionTestUtil.live(later);

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(earlierChronological, currentChronological, laterChronological));

    assertThat(result.get(current.getId()).administratorId()).isEqualTo(1);
    assertThat(result.get(later.getId()).administratorId()).isEqualTo(1);
  }

  @Test
  void resolveStates_whenNoAdministratorChange_isNull() {
    var current = LicencePositionTestUtil.newBuilder().build();
    var currentChronological = ChronologicalPositionTestUtil.live(current);

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(List.of(currentChronological));

    assertThat(result.get(current.getId()).administratorId()).isNull();
  }

  @Test
  void previousState_returnsStateCarriedInBeforeTheGivenPosition() {
    var earlier = LicencePositionTestUtil.newBuilder().build();
    var current = LicencePositionTestUtil.newBuilder().build();

    var earlierChronological = ChronologicalPositionTestUtil.live(
        earlier,
        LicenceOperation.newAdministratorChange().withOperator(1).build()
    );
    var currentChronological = ChronologicalPositionTestUtil.live(
        current,
        LicenceOperation.newAdministratorChange().withOperator(2).build()
    );

    var chronologicalPositions = List.of(earlierChronological, currentChronological);
    var states = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions);

    var previousState = LicencePositionStateResolver.previousState(current.getId(), chronologicalPositions, states);

    assertThat(previousState.administratorId()).isEqualTo(1);
  }

  @Test
  void resolveStates_setEquityChange_replacesHoldersDroppingThoseNotInTheSet() {
    var first = LicencePositionTestUtil.newBuilder().build();
    var second = LicencePositionTestUtil.newBuilder().build();

    var firstChronological = ChronologicalPositionTestUtil.live(
        first,
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("50")).build(),
        LicenceOperation.newSetEquityOperation().withTransferTo(2).withEquity(new BigDecimal("50")).build()
    );
    var secondChronological = ChronologicalPositionTestUtil.live(
        second,
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("30")).build(),
        LicenceOperation.newSetEquityOperation().withTransferTo(3).withEquity(new BigDecimal("70")).build()
    );

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(firstChronological, secondChronological));

    assertThat(result.get(first.getId()).equityByOrganisationId())
        .isEqualTo(Map.of(1, new BigDecimal("50"), 2, new BigDecimal("50")));
    assertThat(result.get(second.getId()).equityByOrganisationId())
        .isEqualTo(Map.of(1, new BigDecimal("30"), 3, new BigDecimal("70")));
  }

  @Test
  void resolveStates_carriesEquityForwardToLaterPositionWithoutChange() {
    var withChange = LicencePositionTestUtil.newBuilder().build();
    var withoutChange = LicencePositionTestUtil.newBuilder().build();

    var withChangeChronological = ChronologicalPositionTestUtil.live(
        withChange,
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("100")).build()
    );
    var withoutChangeChronological = ChronologicalPositionTestUtil.live(withoutChange);

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(withChangeChronological, withoutChangeChronological));

    assertThat(result.get(withoutChange.getId()).equityByOrganisationId())
        .isEqualTo(Map.of(1, new BigDecimal("100")));
  }

  @Test
  void resolveStates_transfer_movesEquityToExistingRecipient() {
    var setPosition = LicencePositionTestUtil.newBuilder().build();
    var transferPosition = LicencePositionTestUtil.newBuilder().build();

    var setChronological = ChronologicalPositionTestUtil.live(
        setPosition,
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("60")).build(),
        LicenceOperation.newSetEquityOperation().withTransferTo(2).withEquity(new BigDecimal("40")).build()
    );
    var transferChronological = ChronologicalPositionTestUtil.live(
        transferPosition,
        LicenceOperation.newTransferEquityOperation()
            .withTransferFrom(1)
            .withTransferTo(2)
            .withEquity(new BigDecimal("10"))
            .build()
    );

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(setChronological, transferChronological));

    assertThat(result.get(transferPosition.getId()).equityByOrganisationId())
        .isEqualTo(Map.of(1, new BigDecimal("50"), 2, new BigDecimal("50")));
  }

  @Test
  void resolveStates_transfer_addsNewRecipientNotAlreadyHoldingEquity() {
    var setPosition = LicencePositionTestUtil.newBuilder().build();
    var transferPosition = LicencePositionTestUtil.newBuilder().build();

    var setChronological = ChronologicalPositionTestUtil.live(
        setPosition,
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("100")).build()
    );
    var transferChronological = ChronologicalPositionTestUtil.live(
        transferPosition,
        LicenceOperation.newTransferEquityOperation()
            .withTransferFrom(1)
            .withTransferTo(3)
            .withEquity(new BigDecimal("30"))
            .build()
    );

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(setChronological, transferChronological));

    assertThat(result.get(transferPosition.getId()).equityByOrganisationId())
        .isEqualTo(Map.of(1, new BigDecimal("70"), 3, new BigDecimal("30")));
  }

  @Test
  void resolveStates_transfer_removesTransferOrLeftWithNoEquity() {
    var setPosition = LicencePositionTestUtil.newBuilder().build();
    var transferPosition = LicencePositionTestUtil.newBuilder().build();

    var setChronological = ChronologicalPositionTestUtil.live(
        setPosition,
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("30")).build(),
        LicenceOperation.newSetEquityOperation().withTransferTo(2).withEquity(new BigDecimal("70")).build()
    );
    var transferChronological = ChronologicalPositionTestUtil.live(
        transferPosition,
        LicenceOperation.newTransferEquityOperation()
            .withTransferFrom(1)
            .withTransferTo(2)
            .withEquity(new BigDecimal("30"))
            .build()
    );

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(setChronological, transferChronological));

    assertThat(result.get(transferPosition.getId()).equityByOrganisationId())
        .isEqualTo(Map.of(2, new BigDecimal("100")));
  }

  @Test
  void resolveStates_transfer_keepsTransferorWithZeroEquityWhenRetainingBeneficialInterest() {
    var setPosition = LicencePositionTestUtil.newBuilder().build();
    var transferPosition = LicencePositionTestUtil.newBuilder().build();

    var setChronological = ChronologicalPositionTestUtil.live(
        setPosition,
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("30")).build(),
        LicenceOperation.newSetEquityOperation().withTransferTo(2).withEquity(new BigDecimal("70")).build()
    );
    var transferChronological = ChronologicalPositionTestUtil.live(
        transferPosition,
        LicenceOperation.newTransferEquityOperation()
            .withTransferFrom(1)
            .withTransferTo(2)
            .withEquity(new BigDecimal("30"))
            .withRetainBeneficialInterest(true)
            .build()
    );

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(setChronological, transferChronological));

    assertThat(result.get(transferPosition.getId()).equityByOrganisationId())
        .isEqualTo(Map.of(1, new BigDecimal("0"), 2, new BigDecimal("100")));
  }

  @Test
  void resolveStates_whenRemoveChange_leavesEquityUntouchedAndCarriesItForward() {
    var setPosition = LicencePositionTestUtil.newBuilder().build();
    var removePosition = LicencePositionTestUtil.newBuilder().build();

    var setChronological = ChronologicalPositionTestUtil.live(
        setPosition,
        LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("50")).build(),
        LicenceOperation.newSetEquityOperation().withTransferTo(2).withEquity(new BigDecimal("50")).build()
    );

    var removeChange = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        LicencePositionChangeType.REMOVE_CHANGE,
        List.of(LicenceOperation.newSetEquityOperation().withTransferTo(3).withEquity(new BigDecimal("100")).build())
    );
    var removeChronological = ChronologicalPosition.fromLicencePosition(
        removePosition, removePosition.getPositionDate(), removePosition.getPositionDateOrder(), List.of(removeChange));

    var result = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(
        List.of(setChronological, removeChronological));

    assertThat(result.get(removePosition.getId()).equityByOrganisationId())
        .isEqualTo(Map.of(1, new BigDecimal("50"), 2, new BigDecimal("50")));
  }

  @Test
  void previousState_whenFirstPosition_isEmpty() {
    var current = LicencePositionTestUtil.newBuilder().build();
    var currentChronological = ChronologicalPositionTestUtil.live(
        current, LicenceOperation.newAdministratorChange().withOperator(2).build());

    var chronologicalPositions = List.of(currentChronological);
    var states = LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions);

    var previousState = LicencePositionStateResolver.previousState(current.getId(), chronologicalPositions, states);

    assertThat(previousState.administratorId()).isNull();
  }
}