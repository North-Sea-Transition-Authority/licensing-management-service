package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

class SubareaEndOperationTest {

  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final UUID FIRST_SUBAREA_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_SUBAREA_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails FIRST_SUBAREA = new SubareaDetails(FIRST_SUBAREA_FEATURE_ID, "Subarea A", "A");
  private static final SubareaDetails SECOND_SUBAREA = new SubareaDetails(SECOND_SUBAREA_FEATURE_ID, "Subarea B", "B");

  @Test
  void constructor_whenIdNull_thenThrows() {
    var endedSubareas = List.of(FIRST_SUBAREA);

    assertThatThrownBy(() -> new SubareaEndOperation(null, BLOCK_FEATURE_ID, endedSubareas))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
  }

  @Test
  void constructor_whenBlockFeatureIdNull_thenThrows() {
    var endedSubareas = List.of(FIRST_SUBAREA);

    assertThatThrownBy(() -> new SubareaEndOperation(null, endedSubareas))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("blockFeatureId");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenEndedSubareasNullOrEmpty_thenEmpty(List<SubareaDetails> endedSubareas) {
    var operation = new SubareaEndOperation(UUID.randomUUID(), BLOCK_FEATURE_ID, endedSubareas);

    assertThat(operation.endedSubareas()).isEmpty();
  }

  @Test
  void constructor_whenUsingConvenienceConstructor_thenGeneratesRandomId() {
    var first = new SubareaEndOperation(BLOCK_FEATURE_ID, List.of(FIRST_SUBAREA));
    var second = new SubareaEndOperation(BLOCK_FEATURE_ID, List.of(FIRST_SUBAREA));

    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void type() {
    var operation = new SubareaEndOperation(BLOCK_FEATURE_ID, List.of(FIRST_SUBAREA));

    assertThat(operation.type()).isEqualTo(LicenceOperation.SUBAREA_END);
  }

  @Test
  void displayName() {
    var operation = new SubareaEndOperation(BLOCK_FEATURE_ID, List.of(FIRST_SUBAREA));

    assertThat(operation.displayName()).isEqualTo("Subareas ended");
  }

  @Test
  void validate() {
    var operation = new SubareaEndOperation(BLOCK_FEATURE_ID, List.of(FIRST_SUBAREA));

    var result = operation.validate(PositionValidationContextTestUtil.newBuilder().build());

    assertThat(result).isNull();
  }

  @Test
  void featureIds_whenSubareasHaveFeatures_thenReturnsBlockAndDistinctSubareaFeatureIds() {
    var operation = new SubareaEndOperation(
        BLOCK_FEATURE_ID,
        List.of(FIRST_SUBAREA, SECOND_SUBAREA, new SubareaDetails(FIRST_SUBAREA_FEATURE_ID, null, "A2"))
    );

    assertThat(operation.featureIds())
        .containsExactlyInAnyOrder(BLOCK_FEATURE_ID, FIRST_SUBAREA_FEATURE_ID, SECOND_SUBAREA_FEATURE_ID);
  }

  @Test
  void featureIds_whenASubareaHasNoFeature_thenItIsOmitted() {
    var operation = new SubareaEndOperation(
        BLOCK_FEATURE_ID,
        List.of(FIRST_SUBAREA, new SubareaDetails(null, "Unscribed", "U"))
    );

    assertThat(operation.featureIds()).containsExactlyInAnyOrder(BLOCK_FEATURE_ID, FIRST_SUBAREA_FEATURE_ID);
  }

  @Test
  void organisationIds() {
    var operation = new SubareaEndOperation(BLOCK_FEATURE_ID, List.of(FIRST_SUBAREA));

    assertThat(operation.organisationUnitIds()).isEmpty();
  }

  @Test
  void build_whenEndedSubareaRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newSubareaEndOperation()
        .withBlockFeatureId(BLOCK_FEATURE_ID)
        .withEndedSubareas(List.of(FIRST_SUBAREA, FIRST_SUBAREA, SECOND_SUBAREA))
        .build();

    var expected = new SubareaEndOperation(operation.id(), BLOCK_FEATURE_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA));
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void build_whenEndedSubareasNotGiven_thenEmpty() {
    var operation = LicenceOperation.newSubareaEndOperation()
        .withBlockFeatureId(BLOCK_FEATURE_ID)
        .build();

    var expected = new SubareaEndOperation(operation.id(), BLOCK_FEATURE_ID, List.of());
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void deserialise_whenTypeIsSubareaEnd_thenReadsSubareaEndOperation() throws Exception {
    var json = """
        {
          "type": "subarea-end",
          "id": "00000000-0000-0000-0000-000000000001",
          "blockFeatureId": "%s",
          "endedSubareas": [
            {"featureId": "%s", "name": "Subarea A", "shortName": "A"},
            {"featureId": null, "name": null, "shortName": "U"}
          ]
        }""".formatted(BLOCK_FEATURE_ID, FIRST_SUBAREA_FEATURE_ID);

    var operation = new ObjectMapper().findAndRegisterModules().readValue(json, LicenceOperation.class);

    var expected = new SubareaEndOperation(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        BLOCK_FEATURE_ID,
        List.of(FIRST_SUBAREA, new SubareaDetails(null, null, "U"))
    );
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void applyState() {
    var state = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA));
    var operation = new SubareaEndOperation(BLOCK_FEATURE_ID, List.of(FIRST_SUBAREA));

    var result = operation.applyState(state);

    var expected = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(SECOND_SUBAREA));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
