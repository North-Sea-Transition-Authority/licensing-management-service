package uk.co.fivium.gisframework.feature;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.fivium.grpc.CoordinateSystem;

class CoordinateSystemUtilsTest {

  @Test
  void testGetWkid_validValues() {
    assertThat(CoordinateSystemUtils.getWkid(CoordinateSystem.ED50)).isEqualTo(4230);
    assertThat(CoordinateSystemUtils.getWkid(CoordinateSystem.BRITISH_NATIONAL_GRID)).isEqualTo(27700);
    assertThat(CoordinateSystemUtils.getWkid(CoordinateSystem.WGS84)).isEqualTo(4326);
  }

  @ParameterizedTest
  @EnumSource(value = CoordinateSystem.class, mode = EXCLUDE, names = {"ED50", "BRITISH_NATIONAL_GRID", "WGS84"})
  void testGetWkid_invalidValues_throwException(CoordinateSystem coordinateSystem) {
    assertThatThrownBy(() -> CoordinateSystemUtils.getWkid(coordinateSystem))
        .isInstanceOf(IllegalArgumentException.class);
  }
}