package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;

class BlockEndOperationTest {

  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();

  @Test
  void constructor_whenIdNull_thenThrows() {
    var endedFeatureIds = List.of(FIRST_FEATURE_ID);

    assertThatThrownBy(() -> new BlockEndOperation(null, endedFeatureIds))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenEndedFeatureIdsNullOrEmpty_thenEmpty(List<UUID> endedFeatureIds) {
    var operation = new BlockEndOperation(UUID.randomUUID(), endedFeatureIds);

    assertThat(operation.endedFeatureIds()).isEmpty();
  }

  @Test
  void constructor_whenUsingConvenienceConstructor_thenGeneratesRandomId() {
    var endedFeatureIds = List.of(FIRST_FEATURE_ID);

    var first = new BlockEndOperation(endedFeatureIds);
    var second = new BlockEndOperation(endedFeatureIds);

    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void type() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID));

    assertThat(operation.type()).isEqualTo(LicenceOperation.BLOCK_END);
  }

  @Test
  void displayName() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID));

    assertThat(operation.displayName()).isEqualTo("Blocks ended");
  }

  @Test
  void validate() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID));

    var result = operation.validate(PositionValidationContextTestUtil.newBuilder().build());

    assertThat(result).isNull();
  }

  @Test
  void featureIds() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID));

    assertThat(LicenceOperation.featureIds(operation)).containsExactly(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
  }

  @Test
  void organisationIds() {
    var operation = new BlockEndOperation(List.of(FIRST_FEATURE_ID));

    assertThat(LicenceOperation.organisationIds(operation)).isEmpty();
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

    var expected = new BlockEndOperation(operation.id(), List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID));
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenEndedFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockEndOperation()
        .withEndedFeatureIds(List.of(FIRST_FEATURE_ID, FIRST_FEATURE_ID))
        .build();

    var expected = new BlockEndOperation(operation.id(), List.of(FIRST_FEATURE_ID));
    assertThat(operation).isEqualTo(expected);
  }
}
