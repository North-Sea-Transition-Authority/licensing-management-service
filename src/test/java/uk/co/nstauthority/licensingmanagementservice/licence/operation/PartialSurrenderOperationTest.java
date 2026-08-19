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

class PartialSurrenderOperationTest {

  private static final LocalDate SURRENDER_DATE = LocalDate.of(2026, 8, 1);
  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();

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
  void constructor_whenBlockSurrenderTypeByFeatureIdNull_thenDefaultsToEmptyMap() {
    var operation = new PartialSurrenderOperation(SURRENDER_DATE, List.of(FIRST_FEATURE_ID), null);

    assertThat(operation.blockSurrenderTypeByFeatureId()).isEmpty();
  }

  @Test
  void constructor_whenBlockSurrenderTypeByFeatureIdProvided_thenRetained() {
    var operation = new PartialSurrenderOperation(
        SURRENDER_DATE, List.of(FIRST_FEATURE_ID), Map.of(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER));

    assertThat(operation.blockSurrenderTypeByFeatureId())
        .containsExactly(entry(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER));
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
