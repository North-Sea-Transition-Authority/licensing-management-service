package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;

class PartialSurrenderOperationTest {

  private static final LocalDate SURRENDER_DATE = LocalDate.of(2026, 8, 1);
  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();
  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();

  private static SurrenderDetails surrenderDetails(BlockSurrenderType type) {
    return new SurrenderDetails(type, UUID.randomUUID(), List.of());
  }

  @Test
  void build_thenFixedOperationId() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();

    assertThat(operation.id()).isEqualTo(PartialSurrenderOperation.PARTIAL_SURRENDER_OPERATION_ID);
  }

  @Test
  void build_whenSurrenderDateGiven_thenGivenDateUsed() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(SURRENDER_DATE)
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();

    var expected = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID), Map.of());
    assertThat(operation).isEqualTo(expected);
  }

  @Test
  void type() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .build();

    assertThat(operation.type()).isEqualTo(LicenceOperation.PARTIAL_SURRENDER);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void constructor_whenFeatureIdsNullOrEmpty_thenThrows(List<UUID> featureIds) {
    assertThatThrownBy(() -> new PartialSurrenderOperation(SURRENDER_DATE, featureIds, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("featureIds must not be null or empty");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void build_whenFeatureIdsNullOrEmpty_thenThrows(List<UUID> featureIds) {
    var builder = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(featureIds);

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("featureIds must not be null or empty");
  }

  @Test
  void build_whenFeatureIdRepeated_thenDeduplicated() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(SURRENDER_DATE)
        .withFeatureIds(List.of(FIRST_FEATURE_ID, FIRST_FEATURE_ID))
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
  void featureIds() {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(SURRENDER_DATE)
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();

    var expected = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID), Map.of());
    assertThat(operation).isEqualTo(expected);
  }
}
