package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;

class BlockCreateOperationTest {

  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();

  @Test
  void constructor_whenIdNull_thenThrows() {
    var featureIds = List.of(FIRST_FEATURE_ID);

    assertThatThrownBy(() -> new BlockCreateOperation(null, featureIds))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenFeatureIdsNullOrEmpty_thenEmpty(List<UUID> featureIds) {
    var operation = new BlockCreateOperation(UUID.randomUUID(), featureIds);

    assertThat(operation.featureIds()).isEmpty();
  }

  @Test
  void constructor_whenUsingConvenienceConstructor_thenGeneratesRandomId() {
    var featureIds = List.of(FIRST_FEATURE_ID);

    var first = new BlockCreateOperation(featureIds);
    var second = new BlockCreateOperation(featureIds);

    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void type() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID));

    assertThat(operation.type()).isEqualTo(LicenceOperation.BLOCK_CREATE);
  }

  @Test
  void displayName() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID));

    assertThat(operation.displayName()).isEqualTo("Blocks created");
  }

  @Test
  void validate() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID));

    var result = operation.validate(PositionValidationContextTestUtil.newBuilder().build());

    assertThat(result).isNull();
  }

  @Test
  void featureIds() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID));

    assertThat(LicenceOperation.featureIds(operation)).containsExactly(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
  }

  @Test
  void organisationIds() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID));

    assertThat(LicenceOperation.organisationIds(operation)).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void build_whenFeatureIdsNullOrEmpty_thenEmpty(List<UUID> featureIds) {
    var operation = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(featureIds)
        .build();

    assertThat(operation.featureIds()).isEmpty();
  }

  @Test
  void build_whenFeatureIdsGiven_thenMatchesConstructor() {
    var operation = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();

    var expected = new BlockCreateOperation(operation.id(), List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID));
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, FIRST_FEATURE_ID))
        .build();

    var expected = new BlockCreateOperation(operation.id(), List.of(FIRST_FEATURE_ID));
    assertThat(operation).isEqualTo(expected);
  }
}
