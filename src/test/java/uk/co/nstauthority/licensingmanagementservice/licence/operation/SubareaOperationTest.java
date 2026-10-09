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

class SubareaOperationTest {

  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final UUID REPLACED_SUBAREA_FEATURE_ID = UUID.randomUUID();
  private static final UUID OUTPUT_SUBAREA_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails REPLACED_SUBAREA = new SubareaDetails(REPLACED_SUBAREA_FEATURE_ID, "Old", "O");
  private static final SubareaDetails OUTPUT_SUBAREA = new SubareaDetails(OUTPUT_SUBAREA_FEATURE_ID, "New", "N");

  @Test
  void constructor_whenIdNull_thenThrows() {
    assertThatThrownBy(() -> new SubareaOperation(null, BLOCK_FEATURE_ID, List.of(), List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  @Test
  void constructor_whenBlockFeatureIdNull_thenThrows() {
    assertThatThrownBy(() -> new SubareaOperation(null, List.of(), List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("blockFeatureId");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenReplacedSubareasNullOrEmpty_thenEmpty(List<SubareaDetails> replacedSubareas) {
    var operation = new SubareaOperation(UUID.randomUUID(), BLOCK_FEATURE_ID, replacedSubareas, List.of(OUTPUT_SUBAREA));

    assertThat(operation.replacedSubareas()).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenOutputSubareasNullOrEmpty_thenEmpty(List<SubareaDetails> outputSubareas) {
    var operation = new SubareaOperation(UUID.randomUUID(), BLOCK_FEATURE_ID, List.of(REPLACED_SUBAREA), outputSubareas);

    assertThat(operation.outputSubareas()).isEmpty();
  }

  @Test
  void constructor_whenUsingConvenienceConstructor_thenGeneratesRandomId() {
    var first = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());
    var second = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());

    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void type() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());

    assertThat(operation.type()).isEqualTo(LicenceOperation.SUBAREA);
  }

  @Test
  void displayName() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());

    assertThat(operation.displayName()).isEqualTo("Subarea change");
  }

  @Test
  void validate() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());

    var result = operation.validate(PositionValidationContextTestUtil.newBuilder().build());

    assertThat(result).isNull();
  }

  @Test
  void featureIds_whenSubareasGiven_thenReturnsBlockReplacedAndOutputFeatureIdsDistinct() {
    var operation = new SubareaOperation(
        BLOCK_FEATURE_ID,
        List.of(REPLACED_SUBAREA, OUTPUT_SUBAREA),
        List.of(OUTPUT_SUBAREA)
    );

    assertThat(operation.featureIds())
        .containsExactlyInAnyOrder(BLOCK_FEATURE_ID, REPLACED_SUBAREA_FEATURE_ID, OUTPUT_SUBAREA_FEATURE_ID);
  }

  @Test
  void featureIds_whenASubareaHasNoFeature_thenItIsOmitted() {
    var operation = new SubareaOperation(
        BLOCK_FEATURE_ID,
        List.of(new SubareaDetails(null, "Unscribed", "U")),
        List.of(OUTPUT_SUBAREA)
    );

    assertThat(operation.featureIds()).containsExactlyInAnyOrder(BLOCK_FEATURE_ID, OUTPUT_SUBAREA_FEATURE_ID);
  }

  @Test
  void organisationIds() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());

    assertThat(operation.organisationUnitIds()).isEmpty();
  }

  @Test
  void build_whenSubareasRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(BLOCK_FEATURE_ID)
        .withReplacedSubareas(List.of(REPLACED_SUBAREA, REPLACED_SUBAREA))
        .withOutputSubareas(List.of(OUTPUT_SUBAREA, OUTPUT_SUBAREA))
        .build();

    var expected = new SubareaOperation(
        operation.id(),
        BLOCK_FEATURE_ID,
        List.of(REPLACED_SUBAREA),
        List.of(OUTPUT_SUBAREA)
    );
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenSubareasNotGiven_thenEmpty() {
    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(BLOCK_FEATURE_ID)
        .build();

    var expected = new SubareaOperation(operation.id(), BLOCK_FEATURE_ID, List.of(), List.of());
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void applyState() {
    var keptSubarea = new SubareaDetails(UUID.randomUUID(), "Kept", "K");
    var state = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(REPLACED_SUBAREA, keptSubarea));
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(REPLACED_SUBAREA), List.of(OUTPUT_SUBAREA));

    var result = operation.applyState(state);

    var expected = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(keptSubarea, OUTPUT_SUBAREA));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
