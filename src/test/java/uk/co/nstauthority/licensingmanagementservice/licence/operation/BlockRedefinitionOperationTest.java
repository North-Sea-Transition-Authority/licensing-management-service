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

class BlockRedefinitionOperationTest {

  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();
  private static final UUID THIRD_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails FIRST_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails SECOND_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
  private static final SubareaDetails UNSCRIBED_SUBAREA = new SubareaDetails(null, "Unscribed", "U");

  @Test
  void constructor_whenIdNull_thenThrows() {
    var replacedFeatureIds = List.of(FIRST_FEATURE_ID);
    var outputFeatureIds = List.of(SECOND_FEATURE_ID);

    assertThatThrownBy(() -> new BlockRedefinitionOperation(
        null,
        replacedFeatureIds,
        outputFeatureIds,
        List.of(),
        Map.of()
    ))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenReplacedFeatureIdsNullOrEmpty_thenEmpty(List<UUID> replacedFeatureIds) {
    var operation = new BlockRedefinitionOperation(UUID.randomUUID(), replacedFeatureIds, List.of(SECOND_FEATURE_ID), List.of(), Map.of());

    assertThat(operation.replacedFeatureIds()).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenOutputFeatureIdsNullOrEmpty_thenEmpty(List<UUID> outputFeatureIds) {
    var operation = new BlockRedefinitionOperation(UUID.randomUUID(), List.of(FIRST_FEATURE_ID), outputFeatureIds, List.of(), Map.of());

    assertThat(operation.outputFeatureIds()).isEmpty();
  }

  @Test
  void constructor_whenUsingConvenienceConstructor_thenGeneratesRandomId() {
    var replacedFeatureIds = List.of(FIRST_FEATURE_ID);
    var outputFeatureIds = List.of(SECOND_FEATURE_ID);

    var first = new BlockRedefinitionOperation(replacedFeatureIds, outputFeatureIds, List.of(), Map.of());
    var second = new BlockRedefinitionOperation(replacedFeatureIds, outputFeatureIds, List.of(), Map.of());

    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void type() {
    var operation = new BlockRedefinitionOperation(List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID), List.of(), Map.of());

    assertThat(operation.type()).isEqualTo(LicenceOperation.BLOCK_REDEFINITION);
  }

  @Test
  void displayName() {
    var operation = new BlockRedefinitionOperation(List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID), List.of(), Map.of());

    assertThat(operation.displayName()).isEqualTo("Block redefinition");
  }

  @Test
  void validate() {
    var operation = new BlockRedefinitionOperation(List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID), List.of(), Map.of());

    var result = operation.validate(PositionValidationContextTestUtil.newBuilder().build());

    assertThat(result).isNull();
  }

  @Test
  void featureIds_returnsDistinctUnionOfReplacedAndOutputFeatureIds() {
    var operation = new BlockRedefinitionOperation(
        List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID),
        List.of(SECOND_FEATURE_ID, THIRD_FEATURE_ID),
        List.of(),
        Map.of()
    );

    assertThat(LicenceOperation.featureIds(operation))
        .containsExactly(FIRST_FEATURE_ID, SECOND_FEATURE_ID, THIRD_FEATURE_ID);
  }

  @Test
  void organisationIds() {
    var operation = new BlockRedefinitionOperation(List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID), List.of(), Map.of());

    assertThat(LicenceOperation.organisationIds(operation)).isEmpty();
  }

  @Test
  void build_whenReplacedFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockRedefinitionOperation()
        .withReplacedFeatureIds(List.of(FIRST_FEATURE_ID, FIRST_FEATURE_ID))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID))
        .build();

    var expected = new BlockRedefinitionOperation(
        operation.id(),
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        List.of(),
        Map.of()
    );
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenOutputFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockRedefinitionOperation()
        .withReplacedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID, SECOND_FEATURE_ID))
        .build();

    var expected = new BlockRedefinitionOperation(
        operation.id(),
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        List.of(),
        Map.of()
    );
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenBothListsGiven_thenMatchesConstructor() {
    var operation = LicenceOperation.newBlockRedefinitionOperation()
        .withReplacedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID))
        .build();

    var expected = new BlockRedefinitionOperation(
        operation.id(),
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        List.of(),
        Map.of()
    );
    assertThat(operation).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenReplacedSubareasNullOrEmpty_thenEmpty(List<SubareaDetails> replacedSubareas) {
    var operation = new BlockRedefinitionOperation(
        UUID.randomUUID(),
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        replacedSubareas,
        Map.of(SECOND_FEATURE_ID, List.of(SECOND_SUBAREA))
    );

    assertThat(operation.replacedSubareas()).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenOutputSubareasNullOrEmpty_thenEmpty(Map<UUID, List<SubareaDetails>> outputSubareas) {
    var operation = new BlockRedefinitionOperation(
        UUID.randomUUID(),
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        List.of(FIRST_SUBAREA),
        outputSubareas
    );

    assertThat(operation.outputFeatureIdToSubareas()).isEmpty();
  }

  @Test
  void outputSubareas_thenReturnsTheSubareasOfEveryOutputBlock() {
    var operation = new BlockRedefinitionOperation(
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID, THIRD_FEATURE_ID),
        List.of(),
        Map.of(SECOND_FEATURE_ID, List.of(FIRST_SUBAREA), THIRD_FEATURE_ID, List.of(SECOND_SUBAREA))
    );

    assertThat(operation.outputSubareas()).containsExactlyInAnyOrder(FIRST_SUBAREA, SECOND_SUBAREA);
  }

  @Test
  void featureIds_whenSubareasGiven_thenReturnsDistinctUnionOfBlocksAndSubareasSkippingThoseWithout() {
    var operation = new BlockRedefinitionOperation(
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        List.of(FIRST_SUBAREA, UNSCRIBED_SUBAREA),
        Map.of(SECOND_FEATURE_ID, List.of(SECOND_SUBAREA, FIRST_SUBAREA))
    );

    assertThat(LicenceOperation.featureIds(operation)).containsExactly(
        FIRST_FEATURE_ID,
        SECOND_FEATURE_ID,
        FIRST_SUBAREA.featureId(),
        SECOND_SUBAREA.featureId()
    );
  }

  @Test
  void build_whenSubareasRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newBlockRedefinitionOperation()
        .withReplacedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withOutputFeatureIds(List.of(SECOND_FEATURE_ID))
        .withReplacedSubareas(List.of(FIRST_SUBAREA, FIRST_SUBAREA))
        .withOutputSubareas(Map.of(SECOND_FEATURE_ID, List.of(SECOND_SUBAREA, SECOND_SUBAREA)))
        .build();

    var expected = new BlockRedefinitionOperation(
        operation.id(),
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        List.of(FIRST_SUBAREA),
        Map.of(SECOND_FEATURE_ID, List.of(SECOND_SUBAREA))
    );
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void serialise_whenOutputSubareasGiven_thenReadsBackTheSame() throws Exception {
    var objectMapper = new ObjectMapper().findAndRegisterModules();
    var operation = new BlockRedefinitionOperation(
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        List.of(FIRST_SUBAREA),
        Map.of(SECOND_FEATURE_ID, List.of(SECOND_SUBAREA, UNSCRIBED_SUBAREA))
    );

    var result = objectMapper.readValue(objectMapper.writeValueAsString(operation), LicenceOperation.class);

    assertThat(result).isEqualTo(operation);
  }
}
