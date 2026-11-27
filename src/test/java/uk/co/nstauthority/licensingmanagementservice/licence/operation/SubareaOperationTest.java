package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import tools.jackson.databind.json.JsonMapper;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SubareaChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureSummary;

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
  void constructor_whenSubareasGiven_thenOneTransfer() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(REPLACED_SUBAREA), List.of(OUTPUT_SUBAREA));

    assertThat(operation.transfers())
        .containsExactly(new SubareaTransfer(List.of(REPLACED_SUBAREA), List.of(OUTPUT_SUBAREA)));
  }

  @Test
  void constructor_whenNoSubareasGiven_thenNoTransfers() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());

    assertThat(operation.transfers()).isEmpty();
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
  void validate_whenNoTransfers_thenNoChangeError() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());
    var context = PositionValidationContextTestUtil.newBuilder().build();

    var result = operation.validate(context);

    assertThat(result).isEqualTo(PositionValidationError.forOperation(
        context,
        LicenceOperation.SUBAREA,
        "Make a change to the subareas"
    ));
  }

  @Test
  void validate_whenTransfersGiven_thenNull() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(REPLACED_SUBAREA), List.of(OUTPUT_SUBAREA));

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
  void replacedSubareas_whenSubareaSplitAcrossTransfers_thenListedOnce() {
    var otherOutputSubarea = new SubareaDetails(UUID.randomUUID(), "Other", "X");
    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(BLOCK_FEATURE_ID)
        .withTransfers(List.of(
            new SubareaTransfer(List.of(REPLACED_SUBAREA), List.of(OUTPUT_SUBAREA)),
            new SubareaTransfer(List.of(REPLACED_SUBAREA), List.of(otherOutputSubarea))
        ))
        .build();

    assertThat(operation.replacedSubareas()).containsExactly(REPLACED_SUBAREA);
  }

  @Test
  void outputSubareas_whenSubareasMergedAcrossTransfers_thenListedOnce() {
    var otherReplacedSubarea = new SubareaDetails(UUID.randomUUID(), "Other", "X");
    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(BLOCK_FEATURE_ID)
        .withTransfers(List.of(
            new SubareaTransfer(List.of(REPLACED_SUBAREA), List.of(OUTPUT_SUBAREA)),
            new SubareaTransfer(List.of(otherReplacedSubarea), List.of(OUTPUT_SUBAREA))
        ))
        .build();

    assertThat(operation.outputSubareas()).containsExactly(OUTPUT_SUBAREA);
  }

  @Test
  void build_whenTransfersNotGiven_thenEmpty() {
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

  @Test
  void build_whenCommandJourneyIdGiven_thenSet() {
    var commandJourneyId = UUID.randomUUID();

    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(BLOCK_FEATURE_ID)
        .withCommandJourneyId(commandJourneyId)
        .build();

    var expected = new SubareaOperation(operation.id(), BLOCK_FEATURE_ID, List.of(), commandJourneyId);
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void commandJourneyIdOrThrow_whenCommandJourneyPresent_thenReturnsId() {
    var commandJourneyId = UUID.randomUUID();
    var operation = new SubareaOperation(UUID.randomUUID(), BLOCK_FEATURE_ID, List.of(), commandJourneyId);

    var result = operation.commandJourneyIdOrThrow();

    assertThat(result).isEqualTo(commandJourneyId);
  }

  @Test
  void commandJourneyIdOrThrow_whenNoCommandJourney_thenThrows() {
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(), List.of());

    assertThatThrownBy(operation::commandJourneyIdOrThrow)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No command journey started for subarea change %s".formatted(operation.id()));
  }

  @Test
  void deserialise_whenPersistedWithoutCommandJourneyId_thenCommandJourneyIdNull() throws Exception {
    var operationId = UUID.randomUUID();
    var json = """
        {
          "type": "subarea",
          "id": "%s",
          "blockFeatureId": "%s",
          "transfers": []
        }""".formatted(operationId, BLOCK_FEATURE_ID);

    var operation = JsonMapper.builder().findAndAddModules().build().readValue(json, LicenceOperation.class);

    var expected = new SubareaOperation(operationId, BLOCK_FEATURE_ID, List.of(), List.of());
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void serialise_whenCommandJourneyIdGiven_thenRoundTrips() throws Exception {
    var operation = new SubareaOperation(
        UUID.randomUUID(),
        BLOCK_FEATURE_ID,
        List.of(new SubareaTransfer(List.of(REPLACED_SUBAREA), List.of(OUTPUT_SUBAREA))),
        UUID.randomUUID()
    );
    var objectMapper = JsonMapper.builder().findAndAddModules().build();

    var result = objectMapper.readValue(objectMapper.writeValueAsString(operation), LicenceOperation.class);

    assertThat(result).isEqualTo(operation);
  }

  @Test
  void getChangeView_whenSubareasSetTransferredAndRemoved_thenRowPerReplacedSubareaSortedByInput() {
    var subareaA = new SubareaDetails(UUID.randomUUID(), "A", "A");
    var subareaB = new SubareaDetails(UUID.randomUUID(), "B", "B");
    var subareaC = new SubareaDetails(UUID.randomUUID(), "C", "C");
    var subareaD = new SubareaDetails(UUID.randomUUID(), "D", "D");
    var subareaE = new SubareaDetails(UUID.randomUUID(), "E", "E");
    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(BLOCK_FEATURE_ID)
        .withTransfers(List.of(
            new SubareaTransfer(List.of(), List.of(subareaA)),
            new SubareaTransfer(List.of(subareaB, subareaC), List.of(subareaD)),
            new SubareaTransfer(List.of(subareaE), List.of())
        ))
        .build();
    var context = new LicencePositionChangeViewContext(Map.of(), Map.of(
        BLOCK_FEATURE_ID, featureSummary("Block 30/1", 6_000_000),
        subareaA.featureId(), featureSummary("A", 1_000_000),
        subareaB.featureId(), featureSummary("B", 2_000_000),
        subareaC.featureId(), featureSummary("C", 3_000_000),
        subareaD.featureId(), featureSummary("D", 5_000_000),
        subareaE.featureId(), featureSummary("E", 8_000_000)
    ));
    var positionChange = PositionChangeTestUtil.newBuilder().build();
    var urls = ChangeViewUrls.none();

    var result = operation.getChangeView(positionChange, LicencePositionState.EMPTY, null, context, urls);

    var expected = new SubareaChangeView(
        "Block 30/1",
        List.of(
            new SubareaChangeView.SubareaRow("", new Tag("Set", TagColour.GREEN), "Subarea A (1.00\u00A0km²)"),
            new SubareaChangeView.SubareaRow(
                "Subarea B (2.00\u00A0km²)", new Tag("Transfer", TagColour.BLUE), "Subarea D (5.00\u00A0km²)"),
            new SubareaChangeView.SubareaRow(
                "Subarea C (3.00\u00A0km²)", new Tag("Transfer", TagColour.BLUE), "Subarea D (5.00\u00A0km²)"),
            new SubareaChangeView.SubareaRow("Subarea E (8.00\u00A0km²)", new Tag("Remove", TagColour.RED), "")
        ),
        positionChange.changeType(),
        urls
    );
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getChangeView_whenTransferUnchanged_thenRowTaggedUnchanged() {
    var subareaA = new SubareaDetails(UUID.randomUUID(), "A", "A");
    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(BLOCK_FEATURE_ID)
        .withTransfers(List.of(new SubareaTransfer(List.of(subareaA), List.of(subareaA))))
        .build();
    var context = new LicencePositionChangeViewContext(Map.of(), Map.of(
        BLOCK_FEATURE_ID, featureSummary("Block 30/1", 6_000_000),
        subareaA.featureId(), featureSummary("A", 1_000_000)
    ));
    var positionChange = PositionChangeTestUtil.newBuilder().build();
    var urls = ChangeViewUrls.none();

    var result = operation.getChangeView(positionChange, LicencePositionState.EMPTY, null, context, urls);

    var expected = new SubareaChangeView(
        "Block 30/1",
        List.of(new SubareaChangeView.SubareaRow(
            "Subarea A (1.00\u00A0km²)", new Tag("Unchanged", TagColour.GREY), "Subarea A (1.00\u00A0km²)")),
        positionChange.changeType(),
        urls
    );
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getChangeView_whenSubareaFeatureUnknown_thenRowsOmitArea() {
    var operation = new SubareaOperation(
        BLOCK_FEATURE_ID,
        List.of(new SubareaDetails(null, "Unknown Area", "UNKNOWN")),
        List.of(OUTPUT_SUBAREA, new SubareaDetails(UUID.randomUUID(), "Other", "X"))
    );
    var context = new LicencePositionChangeViewContext(Map.of(), Map.of(
        BLOCK_FEATURE_ID, featureSummary("Block 30/1", 100_000_000)
    ));
    var positionChange = PositionChangeTestUtil.newBuilder().build();
    var urls = ChangeViewUrls.none();

    var result = operation.getChangeView(positionChange, LicencePositionState.EMPTY, null, context, urls);

    var expected = new SubareaChangeView(
        "Block 30/1",
        List.of(new SubareaChangeView.SubareaRow(
            "Subarea Unknown Area",
            new Tag("Transfer", TagColour.BLUE),
            "Subarea New, Subarea Other"
        )),
        positionChange.changeType(),
        urls
    );
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void doInputsMatchPreviousStateOutputs_whenReplacedSubareasHeldOnBlock_thenTrue() {
    var state = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(REPLACED_SUBAREA));
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(REPLACED_SUBAREA), List.of(OUTPUT_SUBAREA));

    var result = operation.doInputsMatchPreviousStateOutputs(state);

    assertThat(result).isTrue();
  }

  @Test
  void doInputsMatchPreviousStateOutputs_whenReplacedSubareaNotHeld_thenFalse() {
    var state = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(OUTPUT_SUBAREA));
    var operation = new SubareaOperation(BLOCK_FEATURE_ID, List.of(REPLACED_SUBAREA), List.of(OUTPUT_SUBAREA));

    var result = operation.doInputsMatchPreviousStateOutputs(state);

    assertThat(result).isFalse();
  }

  private static FeatureSummary featureSummary(String name, long squareMetres) {
    return new FeatureSummary(4230, name, BigDecimal.valueOf(squareMetres), null, null);
  }
}
