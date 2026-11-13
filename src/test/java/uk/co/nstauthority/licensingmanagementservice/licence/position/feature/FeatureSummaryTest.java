package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FeatureSummaryTest {

  @Test
  void from() {
    var feature = FeatureTestUtil.builder()
        .withFeatureName("SHAPE 7")
        .withFeatureArea(new BigDecimal("12.345"))
        .build();

    var result = FeatureSummary.from(feature);

    assertThat(result).isEqualTo(new FeatureSummary("SHAPE 7", new BigDecimal("12.345")));
  }
}
