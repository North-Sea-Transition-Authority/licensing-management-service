package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

class PartialSurrenderOperationTest {

  private static final LocalDate SURRENDER_DATE = LocalDate.of(2026, Month.AUGUST, 1);
  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();
  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final SubareaDetails FIRST_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails SECOND_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
  private static final SubareaDetails CROPPED_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
  private static final SubareaDetails UNSCRIBED_SUBAREA = new SubareaDetails(null, "Unscribed", "U");
  private static final SubareaDetails THIRD_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea C", "C");
  private static final UUID SURRENDERED_PART_ID = UUID.randomUUID();
  private static final UUID FIRST_RETAINED_PART_ID = UUID.randomUUID();
  private static final UUID SECOND_RETAINED_PART_ID = UUID.randomUUID();

  private static SurrenderDetails surrenderDetails(BlockSurrenderType type) {
    return new SurrenderDetails(type, UUID.randomUUID(), List.of());
  }

  @Test
  void build_thenFixedOperationId() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();

    assertThat(operation.id()).isEqualTo(PartialSurrenderOperation.PARTIAL_SURRENDER_OPERATION_ID);
  }

  @Test
  void build_whenSurrenderDateGiven_thenGivenDateUsed() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(SURRENDER_DATE)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();

    var expected = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID), Map.of());
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void type() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();

    assertThat(operation.type()).isEqualTo(LicenceOperation.PARTIAL_SURRENDER);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenSurrenderedFeatureIdsNullOrEmpty_thenThrows(List<UUID> surrenderedFeatureIds) {
    assertThatThrownBy(() -> new PartialSurrenderOperation(SURRENDER_DATE, surrenderedFeatureIds, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("surrenderedFeatureIds must not be null or empty");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void build_whenSurrenderedFeatureIdsNullOrEmpty_thenThrows(List<UUID> surrenderedFeatureIds) {
    var builder = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(surrenderedFeatureIds);

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("surrenderedFeatureIds must not be null or empty");
  }

  @Test
  void build_whenFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(SURRENDER_DATE)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, FIRST_FEATURE_ID))
        .build();

    var expected = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID), Map.of());
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void constructor_whenBlockSurrendersNull_thenDefaultsToEmptyMap() {
    var operation = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID), null);

    assertThat(operation.featureIdToSurrenderDetails()).isEmpty();
  }

  @Test
  void constructor_whenBlockSurrendersProvided_thenRetained() {
    var blockSurrender = new SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER, COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID));
    var operation = new PartialSurrenderOperation(
        SURRENDER_DATE, List.of(FIRST_FEATURE_ID), Map.of(FIRST_FEATURE_ID, blockSurrender));

    assertThat(operation.featureIdToSurrenderDetails())
        .containsExactly(entry(FIRST_FEATURE_ID, blockSurrender));
  }

  @Test
  void hasUpdateOccurred_whenTheSameBlocksInADifferentOrder_thenFalse() {
    var live = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID), Map.of());
    var corrected = new PartialSurrenderOperation(null, List.of(SECOND_FEATURE_ID, FIRST_FEATURE_ID), Map.of());

    assertThat(corrected.hasUpdateOccurred(live)).isFalse();
  }

  @Test
  void hasUpdateOccurred_whenABlockIsNoLongerSurrendered_thenTrue() {
    var live = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID), Map.of());
    var corrected = new PartialSurrenderOperation(null, List.of(FIRST_FEATURE_ID), Map.of());

    assertThat(corrected.hasUpdateOccurred(live)).isTrue();
  }

  @Test
  void hasUpdateOccurred_whenASurrenderTypeIsSet_thenTrue() {
    var live = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID), Map.of());
    var corrected = new PartialSurrenderOperation(null, List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER)));

    assertThat(corrected.hasUpdateOccurred(live)).isTrue();
  }

  @Test
  void hasUpdateOccurred_whenASurrenderTypeChanges_thenTrue() {
    var live = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER)));
    var corrected = new PartialSurrenderOperation(null, List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.FULL_SURRENDER)));

    assertThat(corrected.hasUpdateOccurred(live)).isTrue();
  }

  @Test
  void hasUpdateOccurred_whenASurrenderTypeMatchesTheLiveSurrenderTypeButTheJourneyDiffers_thenFalse() {
    var live = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER)));
    var corrected = new PartialSurrenderOperation(null, List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER)));

    assertThat(corrected.hasUpdateOccurred(live)).isFalse();
  }

  @Test
  void build_whenSurrenderedFeatureIdsGiven_thenMatchesConstructor() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(SURRENDER_DATE)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();

    var expected = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID), Map.of());
    assertThat(operation).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("surrendersAndWhetherTheyTakeEffect")
  void takesEffect(
      Map<UUID, SurrenderDetails> featureIdToSurrenderDetails,
      boolean expected
  ) {
    var operation = new PartialSurrenderOperation(
        SURRENDER_DATE,
        List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID),
        featureIdToSurrenderDetails
    );

    assertThat(operation.takesEffect()).isEqualTo(expected);
  }

  private static Stream<Arguments> surrendersAndWhetherTheyTakeEffect() {
    var fullSurrender = surrenderDetails(BlockSurrenderType.FULL_SURRENDER);
    var partialSurrenderOnAJourney = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER);
    var partialSurrenderWithAreasSelected = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        COMMAND_JOURNEY_ID,
        List.of(UUID.randomUUID()),
        List.of(UUID.randomUUID())
    );
    var executedPartialSurrender = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(),
        List.of(UUID.randomUUID())
    );

    return Stream.of(
        Arguments.of(Map.of(), false),
        Arguments.of(Map.of(FIRST_FEATURE_ID, fullSurrender), false),
        Arguments.of(Map.of(FIRST_FEATURE_ID, fullSurrender, SECOND_FEATURE_ID, partialSurrenderOnAJourney), false),
        Arguments.of(Map.of(FIRST_FEATURE_ID, fullSurrender, SECOND_FEATURE_ID, fullSurrender), true),
        Arguments.of(Map.of(FIRST_FEATURE_ID, fullSurrender, SECOND_FEATURE_ID, partialSurrenderWithAreasSelected), true),
        Arguments.of(Map.of(FIRST_FEATURE_ID, fullSurrender, SECOND_FEATURE_ID, executedPartialSurrender), true)
    );
  }

  @ParameterizedTest
  @MethodSource("surrenderDetailsAndWhetherTheyAreComplete")
  void isComplete(
      SurrenderDetails surrenderDetails,
      boolean expected
  ) {
    assertThat(surrenderDetails.isComplete()).isEqualTo(expected);
  }

  private static Stream<Arguments> surrenderDetailsAndWhetherTheyAreComplete() {
    return Stream.of(
        Arguments.of(surrenderDetails(BlockSurrenderType.FULL_SURRENDER), true),
        Arguments.of(surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER), false),
        Arguments.of(
            new SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                COMMAND_JOURNEY_ID,
                List.of(FIRST_FEATURE_ID),
                List.of(SECOND_FEATURE_ID)
            ),
            true
        )
    );
  }

  @ParameterizedTest
  @EnumSource(BlockSurrenderType.class)
  void isPartialSurrender(BlockSurrenderType type) {
    var details = surrenderDetails(type);

    assertThat(details.isPartialSurrender()).isEqualTo(type == BlockSurrenderType.PARTIAL_SURRENDER);
  }

  @ParameterizedTest
  @MethodSource("surrendersAndWhetherTheyHaveAPartialSurrender")
  void hasPartialSurrender(
      Map<UUID, SurrenderDetails> featureIdToSurrenderDetails,
      boolean expected
  ) {
    var operation = new PartialSurrenderOperation(
        SURRENDER_DATE,
        List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID),
        featureIdToSurrenderDetails
    );

    assertThat(operation.hasPartialSurrender()).isEqualTo(expected);
  }

  private static Stream<Arguments> surrendersAndWhetherTheyHaveAPartialSurrender() {
    var fullSurrender = surrenderDetails(BlockSurrenderType.FULL_SURRENDER);
    var partialSurrender = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER);

    return Stream.of(
        Arguments.of(Map.of(), false),
        Arguments.of(Map.of(FIRST_FEATURE_ID, fullSurrender, SECOND_FEATURE_ID, fullSurrender), false),
        Arguments.of(Map.of(FIRST_FEATURE_ID, fullSurrender, SECOND_FEATURE_ID, partialSurrender), true)
    );
  }

  @Test
  void deserialise_whenPersistedUnderTheFormerFeatureIdsProperty_thenReadAsSurrenderedFeatureIds() throws Exception {
    var json = """
        {
          "type": "partial-surrender",
          "id": "00000000-0000-0000-0000-000000000001",
          "surrenderDate": "2026-08-01",
          "featureIds": ["%s"]
        }""".formatted(FIRST_FEATURE_ID);

    var operation = new ObjectMapper().findAndRegisterModules().readValue(json, LicenceOperation.class);

    var expected = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID), Map.of());
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void deserialise_whenPersistedBeforeRetainedFeatureIdsExisted_thenDefaultsToEmptyList() throws Exception {
    var json = """
        {
          "type": "partial-surrender",
          "id": "00000000-0000-0000-0000-000000000001",
          "surrenderDate": "2026-08-01",
          "surrenderedFeatureIds": ["%s"],
          "featureIdToSurrenderDetails": {
            "%s": {
              "type": "FULL_SURRENDER",
              "commandJourneyId": null,
              "surrenderedFeatureIds": ["%s"]
            }
          }
        }""".formatted(FIRST_FEATURE_ID, FIRST_FEATURE_ID, FIRST_FEATURE_ID);

    var operation = new ObjectMapper().findAndRegisterModules().readValue(json, LicenceOperation.class);

    var expected = new PartialSurrenderOperation(
        SURRENDER_DATE,
        List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, new SurrenderDetails(BlockSurrenderType.FULL_SURRENDER, null, List.of(FIRST_FEATURE_ID)))
    );
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void commandJourneyIdOrThrow_whenCommandJourneyPresent_thenReturnsId() {
    var details = new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, COMMAND_JOURNEY_ID, List.of());

    var result = details.commandJourneyIdOrThrow();

    assertThat(result).isEqualTo(COMMAND_JOURNEY_ID);
  }

  @Test
  void commandJourneyIdOrThrow_whenNoCommandJourney_thenThrows() {
    var details = new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, null, List.of());

    assertThatThrownBy(details::commandJourneyIdOrThrow)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No split journey started for block surrender");
  }

  @Test
  void surrenderDetailsConstructor_whenSubareasNull_thenEmpty() {
    var details = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, COMMAND_JOURNEY_ID, List.of(), List.of(), null);

    var expected = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, COMMAND_JOURNEY_ID, List.of(), List.of(), Map.of());
    assertThat(details).isEqualTo(expected);
  }

  @Test
  void withSubareas() {
    var details = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER, COMMAND_JOURNEY_ID, List.of(FIRST_FEATURE_ID), List.of(SECOND_FEATURE_ID));
    var retainedFeatureIdToSubareas = Map.of(
        SECOND_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(FIRST_SUBAREA)));

    var result = details.withSubareas(retainedFeatureIdToSubareas);

    var expected = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        COMMAND_JOURNEY_ID,
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        retainedFeatureIdToSubareas
    );
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void withRetainedFeatureIds() {
    var retainedFeatureIdToSubareas = Map.of(
        SECOND_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(FIRST_SUBAREA))
    );
    var details = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        COMMAND_JOURNEY_ID,
        List.of(FIRST_FEATURE_ID),
        List.of(),
        retainedFeatureIdToSubareas
    );

    var result = details.withRetainedFeatureIds(List.of(SECOND_FEATURE_ID));

    var expected = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        COMMAND_JOURNEY_ID,
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID),
        retainedFeatureIdToSubareas
    );
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("areasAndWhetherTheSelectionIsUpToDate")
  void isSelectionUpToDate(
      List<UUID> areaIds,
      boolean expected
  ) {
    var details = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        COMMAND_JOURNEY_ID,
        List.of(FIRST_FEATURE_ID),
        List.of(SECOND_FEATURE_ID)
    );

    assertThat(details.isSelectionUpToDate(areaIds)).isEqualTo(expected);
  }

  private static Stream<Arguments> areasAndWhetherTheSelectionIsUpToDate() {
    return Stream.of(
        Arguments.of(List.of(SECOND_FEATURE_ID, FIRST_FEATURE_ID), true),
        Arguments.of(List.of(FIRST_FEATURE_ID, UUID.randomUUID(), UUID.randomUUID()), false),
        Arguments.of(List.of(UUID.randomUUID(), SECOND_FEATURE_ID), false)
    );
  }

  @Test
  void croppedSubareaFeatureIds() {
    var details = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER).withSubareas(
        Map.of(SECOND_FEATURE_ID, List.of(
            SubareaSurrenderOutcome.relinquished(FIRST_SUBAREA),
            SubareaSurrenderOutcome.cropped(SECOND_SUBAREA, CROPPED_SUBAREA)))
    );

    assertThat(details.croppedSubareaFeatureIds()).containsExactly(CROPPED_SUBAREA.featureId());
  }

  @Test
  void featureIds_whenSurrenderDetailsCarrySubareas_thenIncludesTheirFeatureIdsSkippingThoseWithout() {
    var details = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER).withSubareas(
        Map.of(SECOND_FEATURE_ID, List.of(
            SubareaSurrenderOutcome.relinquished(FIRST_SUBAREA),
            SubareaSurrenderOutcome.cropped(SECOND_SUBAREA, CROPPED_SUBAREA),
            SubareaSurrenderOutcome.kept(UNSCRIBED_SUBAREA)))
    );
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, details))
        .build();

    assertThat(operation.featureIds()).containsExactlyInAnyOrder(
        FIRST_FEATURE_ID,
        FIRST_SUBAREA.featureId(),
        SECOND_SUBAREA.featureId(),
        CROPPED_SUBAREA.featureId()
    );
  }

  @Test
  void serialise_whenSurrenderDetailsCarrySubareas_thenRoundTrips() throws Exception {
    var details = surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER).withSubareas(
        Map.of(SECOND_FEATURE_ID, List.of(
            SubareaSurrenderOutcome.relinquished(FIRST_SUBAREA),
            SubareaSurrenderOutcome.cropped(SECOND_SUBAREA, CROPPED_SUBAREA)))
    );
    LicenceOperation operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(SURRENDER_DATE)
        .withSurrenderedFeatureIds(List.of(FIRST_FEATURE_ID))
        .withSurrenderDetails(Map.of(FIRST_FEATURE_ID, details))
        .build();
    var objectMapper = new ObjectMapper().findAndRegisterModules();

    var result = objectMapper.readValue(objectMapper.writeValueAsString(operation), LicenceOperation.class);

    assertThat(result).isEqualTo(operation);
  }

  @Test
  void applyState_whenSurrenderTakesEffect_thenSurrenderedBlocksSwappedForThePartsKeptWithTheirSubareas() {
    var keptBlockId = UUID.randomUUID();
    var retainedFeatureId = UUID.randomUUID();
    var state = LicencePositionState.EMPTY
        .withBlock(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA, UNSCRIBED_SUBAREA))
        .withBlock(SECOND_FEATURE_ID, List.of())
        .withBlock(keptBlockId, List.of());
    var partialSurrenderDetails = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(),
        List.of(retainedFeatureId),
        Map.of(retainedFeatureId, List.of(
            SubareaSurrenderOutcome.relinquished(FIRST_SUBAREA),
            SubareaSurrenderOutcome.cropped(SECOND_SUBAREA, CROPPED_SUBAREA),
            SubareaSurrenderOutcome.kept(UNSCRIBED_SUBAREA)
        ))
    );
    var fullSurrenderDetails = new SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER,
        null,
        List.of(SECOND_FEATURE_ID)
    );
    var operation = new PartialSurrenderOperation(
        SURRENDER_DATE,
        List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, partialSurrenderDetails, SECOND_FEATURE_ID, fullSurrenderDetails)
    );

    var result = operation.applyState(state);

    var expected = LicencePositionState.EMPTY
        .withBlock(keptBlockId, List.of())
        .withBlock(retainedFeatureId, List.of(CROPPED_SUBAREA, UNSCRIBED_SUBAREA));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void applyState_whenSurrenderHasNotTakenEffect_thenUnchanged() {
    var state = LicencePositionState.EMPTY.withBlock(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA));
    var operation = new PartialSurrenderOperation(
        SURRENDER_DATE,
        List.of(FIRST_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, surrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER))
    );

    var result = operation.applyState(state);

    assertThat(result).usingRecursiveComparison().isEqualTo(state);
  }

  @Test
  void doInputsMatchPreviousStateOutputs_whenSurrenderedBlocksAndOutcomeSubareasHeld_thenTrue() {
    var state = LicencePositionState.EMPTY
        .withBlock(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA, SECOND_SUBAREA))
        .withBlock(SECOND_FEATURE_ID, List.of());

    var result = surrenderCroppingSecondSubarea().doInputsMatchPreviousStateOutputs(state);

    assertThat(result).isTrue();
  }

  @Test
  void doInputsMatchPreviousStateOutputs_whenOutcomeSubareaNotHeld_thenFalse() {
    var state = LicencePositionState.EMPTY
        .withBlock(FIRST_FEATURE_ID, List.of(FIRST_SUBAREA, CROPPED_SUBAREA))
        .withBlock(SECOND_FEATURE_ID, List.of());

    var result = surrenderCroppingSecondSubarea().doInputsMatchPreviousStateOutputs(state);

    assertThat(result).isFalse();
  }

  @Test
  void createdFeatureIds_thenThePartsSplitFromTheBlocksAndTheSubareasCroppedToThem() {
    var result = surrenderOfAPartAndAWholeBlock().createdFeatureIds();

    assertThat(result).containsExactlyInAnyOrder(
        SURRENDERED_PART_ID,
        FIRST_RETAINED_PART_ID,
        SECOND_RETAINED_PART_ID,
        CROPPED_SUBAREA.featureId()
    );
  }

  @Test
  void endedFeatureIds_thenTheBlocksAndPartsGivenUpAndTheSubareasNotKeptOnAnyPart() {
    var result = surrenderOfAPartAndAWholeBlock().endedFeatureIds();

    assertThat(result).containsExactlyInAnyOrder(
        FIRST_FEATURE_ID,
        SECOND_FEATURE_ID,
        SURRENDERED_PART_ID,
        SECOND_SUBAREA.featureId(),
        THIRD_SUBAREA.featureId()
    );
  }

  private static PartialSurrenderOperation surrenderOfAPartAndAWholeBlock() {
    var partialSurrenderDetails = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(SURRENDERED_PART_ID),
        List.of(FIRST_RETAINED_PART_ID, SECOND_RETAINED_PART_ID),
        Map.of(
            FIRST_RETAINED_PART_ID, List.of(
                SubareaSurrenderOutcome.kept(FIRST_SUBAREA),
                SubareaSurrenderOutcome.cropped(SECOND_SUBAREA, CROPPED_SUBAREA),
                SubareaSurrenderOutcome.kept(UNSCRIBED_SUBAREA)
            ),
            SECOND_RETAINED_PART_ID, List.of(
                SubareaSurrenderOutcome.relinquished(FIRST_SUBAREA),
                SubareaSurrenderOutcome.relinquished(SECOND_SUBAREA),
                SubareaSurrenderOutcome.relinquished(UNSCRIBED_SUBAREA)
            )
        )
    );
    var fullSurrenderDetails = new SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER,
        null,
        List.of(SECOND_FEATURE_ID),
        List.of(),
        Map.of(SECOND_FEATURE_ID, List.of(SubareaSurrenderOutcome.relinquished(THIRD_SUBAREA)))
    );
    return new PartialSurrenderOperation(
        SURRENDER_DATE,
        List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, partialSurrenderDetails, SECOND_FEATURE_ID, fullSurrenderDetails)
    );
  }

  private static PartialSurrenderOperation surrenderCroppingSecondSubarea() {
    var retainedFeatureId = UUID.randomUUID();
    var surrenderDetails = new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        COMMAND_JOURNEY_ID,
        List.of(),
        List.of(retainedFeatureId),
        Map.of(retainedFeatureId, List.of(
            SubareaSurrenderOutcome.relinquished(FIRST_SUBAREA),
            SubareaSurrenderOutcome.cropped(SECOND_SUBAREA, CROPPED_SUBAREA)))
    );
    return new PartialSurrenderOperation(
        SURRENDER_DATE,
        List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID),
        Map.of(FIRST_FEATURE_ID, surrenderDetails)
    );
  }
}
