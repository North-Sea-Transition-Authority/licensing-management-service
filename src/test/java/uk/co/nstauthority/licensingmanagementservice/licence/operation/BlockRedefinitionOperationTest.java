package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;

class BlockRedefinitionOperationTest {

  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();
  private static final UUID THIRD_FEATURE_ID = UUID.randomUUID();

  @Test
  void constructor_whenIdNull_thenThrows() {
    var replacedFeatureIds = List.of(FIRST_FEATURE_ID);
    var outputFeatureIds = List.of(SECOND_FEATURE_ID);

    assertThatThrownBy(() -> new BlockRedefinitionOperation(null, replacedFeatureIds, outputFeatureIds))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenReplacedFeatureIdsNullOrEmpty_thenEmpty(List<UUID> replacedFeatureIds) {
    var operation = new BlockRedefinitionOperation(UUID.randomUUID(), replacedFeatureIds, List.of(SECOND_FEATURE_ID));

    assertThat(operation.replacedFeatureIds()).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenOutputFeatureIdsNullOrEmpty_thenEmpty(List<UUID> outputFeatureIds) {
    var operation = new BlockRedefinitionOperation(UUID.randomUUID(), List.of(FIRST_FEATURE_ID), outputFeatureIds);

    assertThat(operation.outputFeatureIds()).isEmpty();
  }

  @Test
  void constructor_whenUsingConvenienceConstructor_thenGeneratesRandomId() {
    var replacedFeatureIds = List.of(FIRST_FEATURE_ID);
    var outputFeatureIds = List.of(SECOND_FEATURE_ID);

    var first = new BlockRedefinitionOperation(replacedFeatureIds, outputFeatureIds);
    var second = new BlockRedefinitionOperation(replacedFeatureIds, outputFeatureIds);

    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void type() {
    var operation = new BlockRedefinitionOperation(List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID));

    assertThat(operation.type()).isEqualTo(LicenceOperation.BLOCK_REDEFINITION);
  }

  @Test
  void displayName() {
    var operation = new BlockRedefinitionOperation(List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID));

    assertThat(operation.displayName()).isEqualTo("Block redefinition");
  }

  @Test
  void validate() {
    var operation = new BlockRedefinitionOperation(List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID));

    var result = operation.validate(PositionValidationContextTestUtil.newBuilder().build());

    assertThat(result).isNull();
  }

  @Test
  void featureIds_returnsDistinctUnionOfReplacedAndOutputFeatureIds() {
    var operation = new BlockRedefinitionOperation(
        List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID),
        List.of(SECOND_FEATURE_ID, THIRD_FEATURE_ID));

    assertThat(LicenceOperation.featureIds(operation))
        .containsExactly(FIRST_FEATURE_ID, SECOND_FEATURE_ID, THIRD_FEATURE_ID);
  }

  @Test
  void organisationIds() {
    var operation = new BlockRedefinitionOperation(List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID));

    assertThat(LicenceOperation.organisationIds(operation)).isEmpty();
  }

  @Test
  void build_whenReplacedFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockRedefinitionOperation()
        .withReplacedFeatureIds(List.of(FIRST_FEATURE_ID, FIRST_FEATURE_ID))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID))
        .build();

    var expected = new BlockRedefinitionOperation(
        operation.id(), List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID));
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenOutputFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockRedefinitionOperation()
        .withReplacedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID, SECOND_FEATURE_ID))
        .build();

    var expected = new BlockRedefinitionOperation(
        operation.id(), List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID));
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenBothListsGiven_thenMatchesConstructor() {
    var operation = LicenceOperation.newBlockRedefinitionOperation()
        .withReplacedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID))
        .build();

    var expected = new BlockRedefinitionOperation(
        operation.id(), List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID));
    assertThat(operation).isEqualTo(expected);
  }
}
