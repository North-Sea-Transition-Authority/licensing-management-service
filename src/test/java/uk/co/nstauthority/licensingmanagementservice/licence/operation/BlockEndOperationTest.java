package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

class BlockEndOperationTest {

  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails FIRST_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails SECOND_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
  private static final SubareaDetails UNSCRIBED_SUBAREA = new SubareaDetails(null, "Unscribed", "U");

  @Test
  void constructor_whenIdNull_thenThrows() {
    var endedFeatureIds = List.of(FIRST_FEATURE_ID);

    assertThatThrownBy(() -> new BlockEndOperation(null, endedFeatureIds, List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenEndedFeatureIdsNullOrEmpty_thenEmpty(List<UUID> endedFeatureIds) {
    var operation = new BlockEndOperation(UUID.randomUUID(), endedFeatureIds, List.of());

    assertThat(operation.endedFeatureIds()).isEmpty();
  }

  @Test
  void constructor_whenUsingConvenienceConstructor_thenGeneratesRandomId() {
    var endedFeatureIds = List.of(FIRST_FEATURE_ID);

    var first = new BlockEndOperation(endedFeatureIds, List.of());
    var second = new BlockEndOperation(endedFeatureIds, List.of());

    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void type() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID), List.of());

    assertThat(operation.type()).isEqualTo(LicenceOperation.BLOCK_END);
  }

  @Test
  void displayName() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID), List.of());

    assertThat(operation.displayName()).isEqualTo("Blocks ended");
  }

  @Test
  void validate() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID), List.of());

    var result = operation.validate(PositionValidationContextTestUtil.newBuilder().build());

    assertThat(result).isNull();
  }

  @Test
  void featureIds() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID), List.of());

    assertThat(operation.featureIds()).containsExactlyInAnyOrder(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
  }

  @Test
  void organisationIds() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID), List.of());

    assertThat(operation.organisationUnitIds()).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void build_whenEndedFeatureIdsNullOrEmpty_thenEmpty(List<UUID> endedFeatureIds) {
    var operation = LicenceOperation.newBlockEndOperation()
        .withEndedFeatureIds(endedFeatureIds)
        .build();

    assertThat(operation.endedFeatureIds()).isEmpty();
  }

  @Test
  void build_whenEndedFeatureIdsGiven_thenMatchesConstructor() {
    var operation = LicenceOperation.newBlockEndOperation()
        .withEndedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();

    var expected = new BlockEndOperation(operation.id(), List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID), List.of());
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenEndedFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockEndOperation()
        .withEndedFeatureIds(List.of(FIRST_FEATURE_ID, FIRST_FEATURE_ID))
        .build();

    var expected = new BlockEndOperation(operation.id(), List.of(FIRST_FEATURE_ID), List.of());
    assertThat(operation).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenEndedSubareasNullOrEmpty_thenEmpty(List<SubareaDetails> endedSubareas) {
    var operation = new BlockEndOperation(UUID.randomUUID(), List.of(FIRST_FEATURE_ID), endedSubareas);

    assertThat(operation.endedSubareas()).isEmpty();
  }

  @Test
  void featureIds_whenEndedSubareasGiven_thenIncludesTheirFeatureIdsSkippingThoseWithout() {
    var operation = new BlockEndOperation(
        List.of(FIRST_FEATURE_ID),
        List.of(FIRST_SUBAREA, UNSCRIBED_SUBAREA)
    );

    assertThat(operation.featureIds()).containsExactlyInAnyOrder(FIRST_FEATURE_ID, FIRST_SUBAREA.featureId());
  }

  @Test
  void build_whenEndedSubareaRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockEndOperation()
        .withEndedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withEndedSubareas(List.of(FIRST_SUBAREA, FIRST_SUBAREA, SECOND_SUBAREA))
        .build();

    var expected = new BlockEndOperation(
        operation.id(),
        List.of(FIRST_FEATURE_ID),
        List.of(FIRST_SUBAREA, SECOND_SUBAREA)
    );
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void applyState() {
    var state = LicencePositionState.EMPTY
        .withBlock(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA))
        .withBlock(SECOND_FEATURE_ID, List.of(SECOND_SUBAREA));
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID), List.of(FIRST_SUBAREA));

    var result = operation.applyState(state);

    var expected = LicencePositionState.EMPTY.withBlock(SECOND_FEATURE_ID, List.of(SECOND_SUBAREA));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
