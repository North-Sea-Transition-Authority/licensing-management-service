package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class LicenceBlockFeatureUtilTest {

  @Test
  void isBlock_whenLayerIsBlocks_assertTrue() {
    var feature = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);

    assertThat(LicenceBlockFeatureUtil.isLicenceBlock(feature)).isTrue();
  }

  @Test
  void isBlock_whenLayerIsNotBlocks_assertFalse() {
    var feature = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "Subarea A");

    assertThat(LicenceBlockFeatureUtil.isLicenceBlock(feature)).isFalse();
  }

  @NullAndEmptySource
  @ParameterizedTest
  void isBlock_whenNoAttributes_assertFalse(Map<String, String> attributes) {
    var feature = FeatureTestUtil.builder().withAttributes(attributes).build();

    assertThat(LicenceBlockFeatureUtil.isLicenceBlock(feature)).isFalse();
  }

  @Test
  void blockOrder_ordersNumericQuadrantsAndBlocksAscendingAheadOfLetteredQuadrants() {
    var onshore = FeatureTestUtil.blockFeature(UUID.randomUUID(), "SU", 1);
    var quadrant211Block1 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "211", 1);
    var quadrant30Block2 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);
    var quadrant30Block10 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 10);

    var result = Stream.of(onshore, quadrant30Block10, quadrant211Block1, quadrant30Block2)
        .sorted(LicenceBlockFeatureUtil.BLOCK_ORDER)
        .toList();

    assertThat(result).containsExactly(quadrant30Block2, quadrant30Block10, quadrant211Block1, onshore);
  }

  @Test
  void blockOrder_whenQuadrantOrBlockAbsent_ordersAbsentPartsLast() {
    var absentQuadrant = FeatureTestUtil.builder().withId(UUID.randomUUID()).build();
    var onshore = FeatureTestUtil.blockFeature(UUID.randomUUID(), "SU", 1);
    var quadrant30Block1 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);

    var result = Stream.of(absentQuadrant, onshore, quadrant30Block1)
        .sorted(LicenceBlockFeatureUtil.BLOCK_ORDER)
        .toList();

    assertThat(result).containsExactly(quadrant30Block1, onshore, absentQuadrant);
  }
}
