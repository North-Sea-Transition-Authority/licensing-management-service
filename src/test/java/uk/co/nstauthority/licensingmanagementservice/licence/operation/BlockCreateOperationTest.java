package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

class BlockCreateOperationTest {

  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails FIRST_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails SECOND_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
  private static final SubareaDetails UNSCRIBED_SUBAREA = new SubareaDetails(null, "Unscribed", "U");

  @Test
  void constructor_whenIdNull_thenThrows() {
    var featureIds = List.of(FIRST_FEATURE_ID);

    assertThatThrownBy(() -> new BlockCreateOperation(null, featureIds, Map.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenFeatureIdsNullOrEmpty_thenEmpty(List<UUID> featureIds) {
    var operation = new BlockCreateOperation(UUID.randomUUID(), featureIds, Map.of());

    assertThat(operation.createdBlockFeatureIds()).isEmpty();
  }

  @Test
  void constructor_whenUsingConvenienceConstructor_thenGeneratesRandomId() {
    var featureIds = List.of(FIRST_FEATURE_ID);

    var first = new BlockCreateOperation(featureIds, Map.of());
    var second = new BlockCreateOperation(featureIds, Map.of());

    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void type() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID), Map.of());

    assertThat(operation.type()).isEqualTo(LicenceOperation.BLOCK_CREATE);
  }

  @Test
  void displayName() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID), Map.of());

    assertThat(operation.displayName()).isEqualTo("Blocks created");
  }

  @Test
  void validate() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID), Map.of());

    var result = operation.validate(PositionValidationContextTestUtil.newBuilder().build());

    assertThat(result).isNull();
  }

  @Test
  void featureIds() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID), Map.of());

    assertThat(operation.featureIds()).containsExactlyInAnyOrder(FIRST_FEATURE_ID, SECOND_FEATURE_ID);
  }

  @Test
  void organisationIds() {
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID), Map.of());

    assertThat(operation.organisationUnitIds()).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void build_whenFeatureIdsNullOrEmpty_thenEmpty(List<UUID> featureIds) {
    var operation = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(featureIds)
        .build();

    assertThat(operation.createdBlockFeatureIds()).isEmpty();
  }

  @Test
  void build_whenFeatureIdsGiven_thenMatchesConstructor() {
    var operation = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();

    var expected = new BlockCreateOperation(operation.id(), List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID), Map.of());
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, FIRST_FEATURE_ID))
        .build();

    var expected = new BlockCreateOperation(operation.id(), List.of(FIRST_FEATURE_ID), Map.of());
    assertThat(operation).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenCreatedSubareasNullOrEmpty_thenEmpty(Map<UUID, List<SubareaDetails>> createdSubareas) {
    var operation = new BlockCreateOperation(UUID.randomUUID(), List.of(FIRST_FEATURE_ID), createdSubareas);

    assertThat(operation.createdBlockFeatureIdToSubareas()).isEmpty();
  }

  @Test
  void createdSubareas_thenReturnsTheSubareasOfEveryBlock() {
    var operation = new BlockCreateOperation(
        List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA), SECOND_FEATURE_ID, List.of(SECOND_SUBAREA))
    );

    assertThat(operation.createdSubareas()).containsExactlyInAnyOrder(FIRST_SUBAREA, SECOND_SUBAREA);
  }

  @Test
  void featureIds_whenCreatedSubareasGiven_thenIncludesTheirFeatureIdsSkippingThoseWithout() {
    var operation = new BlockCreateOperation(
        List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA, UNSCRIBED_SUBAREA))
    );

    assertThat(operation.featureIds()).containsExactlyInAnyOrder(FIRST_FEATURE_ID, FIRST_SUBAREA.featureId());
  }

  @Test
  void build_whenCreatedSubareaRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .withCreatedSubareas(Map.of(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA, FIRST_SUBAREA, SECOND_SUBAREA)))
        .build();

    var expected = new BlockCreateOperation(
        operation.id(),
        List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA))
    );
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void serialise_whenCreatedSubareasGiven_thenReadsBackTheSame() throws Exception {
    var objectMapper = new ObjectMapper().findAndRegisterModules();
    var operation = new BlockCreateOperation(
        List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA, UNSCRIBED_SUBAREA))
    );

    var result = objectMapper.readValue(objectMapper.writeValueAsString(operation), LicenceOperation.class);

    assertThat(result).isEqualTo(operation);
  }

  @Test
  void serialise_thenCreatedBlockFeatureIdsWrittenAsFeatureIds() throws Exception {
    var objectMapper = new ObjectMapper().findAndRegisterModules();
    var operation = new BlockCreateOperation(List.of(FIRST_FEATURE_ID), Map.of());

    var json = objectMapper.readTree(objectMapper.writeValueAsString(operation));

    assertThat(json.get("featureIds").get(0).asText()).isEqualTo(FIRST_FEATURE_ID.toString());
    assertThat(json.has("createdBlockFeatureIds")).isFalse();
  }

  @Test
  void deserialise_whenPersistedBeforeSubareasExisted_thenReadsFeatureIdsAndDefaultsSubareasToEmpty() throws Exception {
    var json = """
        {
          "type": "block-create",
          "id": "00000000-0000-0000-0000-000000000001",
          "featureIds": ["%s"]
        }""".formatted(FIRST_FEATURE_ID);

    var operation = new ObjectMapper().findAndRegisterModules().readValue(json, LicenceOperation.class);

    var expected = new BlockCreateOperation(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        List.of(FIRST_FEATURE_ID),
        Map.of()
    );
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void applyState_whenBlocksCreated_thenAddedWithTheSubareasTheyArriveWith() {
    var heldBlockId = UUID.randomUUID();
    var state = LicencePositionState.EMPTY.withBlock(heldBlockId, List.of());
    var operation = new BlockCreateOperation(
        List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA))
    );

    var result = operation.applyState(state);

    var expected = state
        .withBlock(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA))
        .withBlock(SECOND_FEATURE_ID, List.of());
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
