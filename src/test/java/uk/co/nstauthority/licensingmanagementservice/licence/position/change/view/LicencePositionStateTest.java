package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;

class LicencePositionStateTest {

  private static final UUID FIRST_BLOCK_ID = UUID.randomUUID();
  private static final UUID SECOND_BLOCK_ID = UUID.randomUUID();
  private static final UUID NOT_HELD_BLOCK_ID = UUID.randomUUID();
  private static final SubareaDetails FIRST_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails SECOND_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
  private static final SubareaDetails THIRD_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea C", "C");

  @Test
  void subareasOf_whenBlockNotHeld_thenEmpty() {
    var state = LicencePositionState.EMPTY.withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA));

    assertThat(state.subareasOf(NOT_HELD_BLOCK_ID)).isEmpty();
  }

  @Test
  void withBlock_whenBlockNotHeld_thenHeldWithItsSubareas() {
    var state = LicencePositionState.EMPTY.withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA));

    assertThat(state.blockFeatureIds()).containsExactly(FIRST_BLOCK_ID);
    assertThat(state.subareasOf(FIRST_BLOCK_ID)).containsExactly(FIRST_SUBAREA, SECOND_SUBAREA);
  }

  @Test
  void withBlock_whenBlockAlreadyHeld_thenSubareasAddedInOrderWithoutDuplicates() {
    var state = LicencePositionState.EMPTY.withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA));

    var result = state.withBlock(FIRST_BLOCK_ID, List.of(SECOND_SUBAREA, THIRD_SUBAREA));

    assertThat(result.blockFeatureIds()).containsExactly(FIRST_BLOCK_ID);
    assertThat(result.subareasOf(FIRST_BLOCK_ID)).containsExactly(FIRST_SUBAREA, SECOND_SUBAREA, THIRD_SUBAREA);
  }

  @Test
  void withBlock_thenOriginalStateUnchanged() {
    var state = LicencePositionState.EMPTY.withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA));

    state.withBlock(FIRST_BLOCK_ID, List.of(SECOND_SUBAREA));
    state.withBlock(SECOND_BLOCK_ID, List.of());

    assertThat(state.blockFeatureIds()).containsExactly(FIRST_BLOCK_ID);
    assertThat(state.subareasOf(FIRST_BLOCK_ID)).containsExactly(FIRST_SUBAREA);
  }

  @Test
  void withBlock_thenOrganisationStateKept() {
    var state = LicencePositionState.EMPTY
        .withAdministratorId(1)
        .withLicenseeIds(List.of(1, 2), List.of())
        .withEquityByOrganisationId(Map.of(1, BigDecimal.TEN));

    var result = state.withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA));

    var expected = new LicencePositionState(
        1,
        Set.of(1, 2),
        Map.of(1, BigDecimal.TEN),
        Map.of(FIRST_BLOCK_ID, Set.of(FIRST_SUBAREA))
    );
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void withoutBlocks_whenSomeBlocksNotHeld_thenOnlyHeldBlocksRemoved() {
    var state = LicencePositionState.EMPTY
        .withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA))
        .withBlock(SECOND_BLOCK_ID, List.of(SECOND_SUBAREA));

    var result = state.withoutBlocks(List.of(FIRST_BLOCK_ID, NOT_HELD_BLOCK_ID));

    var expected = LicencePositionState.EMPTY.withBlock(SECOND_BLOCK_ID, List.of(SECOND_SUBAREA));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void withSubareas_whenBlockHeld_thenSubareasAdded() {
    var state = LicencePositionState.EMPTY.withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA));

    var result = state.withSubareas(FIRST_BLOCK_ID, List.of(SECOND_SUBAREA));

    assertThat(result.subareasOf(FIRST_BLOCK_ID)).containsExactly(FIRST_SUBAREA, SECOND_SUBAREA);
  }

  @Test
  void withSubareas_whenBlockNotHeld_thenUnchanged() {
    var state = LicencePositionState.EMPTY.withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA));

    var result = state.withSubareas(NOT_HELD_BLOCK_ID, List.of(SECOND_SUBAREA));

    assertThat(result).usingRecursiveComparison().isEqualTo(state);
  }

  @Test
  void withoutSubareas_whenBlockHeld_thenSubareasRemoved() {
    var state = LicencePositionState.EMPTY.withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA));

    var result = state.withoutSubareas(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA));

    assertThat(result.blockFeatureIds()).containsExactly(FIRST_BLOCK_ID);
    assertThat(result.subareasOf(FIRST_BLOCK_ID)).containsExactly(SECOND_SUBAREA);
  }

  @Test
  void withoutSubareas_whenBlockNotHeld_thenUnchanged() {
    var state = LicencePositionState.EMPTY.withBlock(FIRST_BLOCK_ID, List.of(FIRST_SUBAREA));

    var result = state.withoutSubareas(NOT_HELD_BLOCK_ID, List.of(FIRST_SUBAREA));

    assertThat(result).usingRecursiveComparison().isEqualTo(state);
  }
}
