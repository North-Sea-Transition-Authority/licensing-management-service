package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

class SubareaDetailsTest {

  @Test
  void from() {
    var feature = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "A");

    var result = SubareaDetails.from(feature);

    assertThat(result).isEqualTo(new SubareaDetails(feature.getId(), "SHAPE A", "A"));
  }
}
