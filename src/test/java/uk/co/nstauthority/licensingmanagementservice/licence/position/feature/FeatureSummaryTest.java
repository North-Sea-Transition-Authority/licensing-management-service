package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import uk.co.fivium.grpc.gis.CoordinateSystem;

class FeatureSummaryTest {

  @Test
  void from_whenBlockAttributesPresent_thenSummaryIncludesQuadrantAndBlockNumbers() {
    var feature = FeatureTestUtil.builder()
        .withFeatureName("SHAPE 7")
        .withFeatureArea(new BigDecimal("12.345"))
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withAttributes(Map.of("QUADRANT_NO", "30", "BLOCK_NO", "7"))
        .build();

    var result = FeatureSummary.from(feature);

    assertThat(result).isEqualTo(new FeatureSummary(27700, "SHAPE 7", new BigDecimal("12.345"), "30", "7"));
  }

  @Test
  void from_whenNoAttributes_thenQuadrantAndBlockNumbersNull() {
    var feature = FeatureTestUtil.builder()
        .withFeatureName("SHAPE 7")
        .withFeatureArea(new BigDecimal("12.345"))
        .withAttributes(null)
        .build();

    var result = FeatureSummary.from(feature);

    assertThat(result).isEqualTo(new FeatureSummary(4230, "SHAPE 7", new BigDecimal("12.345"), null, null));
  }

  @Test
  void blockOrder_ordersNumericallyByQuadrantThenBlockWithAbsentPartsLast() {
    var absentQuadrant = new FeatureSummary(4230, "absent", BigDecimal.ONE, null, null);
    var onshore = new FeatureSummary(4230, "SU/1", BigDecimal.ONE, "SU", "1");
    var quadrant211Block1 = new FeatureSummary(4230, "211/1", BigDecimal.ONE, "211", "1");
    var quadrant30Block2 = new FeatureSummary(4230, "30/2", BigDecimal.ONE, "30", "2");
    var quadrant30Block10 = new FeatureSummary(4230, "30/10", BigDecimal.ONE, "30", "10");

    var result = Stream.of(absentQuadrant, onshore, quadrant30Block10, quadrant211Block1, quadrant30Block2)
        .sorted(FeatureSummary.BLOCK_ORDER)
        .toList();

    assertThat(result).containsExactly(quadrant30Block2, quadrant30Block10, quadrant211Block1, onshore, absentQuadrant);
  }
}
