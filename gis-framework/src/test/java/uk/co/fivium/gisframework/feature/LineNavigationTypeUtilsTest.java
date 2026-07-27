package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.fivium.grpc.gis.LineNavigationType;

class LineNavigationTypeUtilsTest {

  @Test
  void getDisplayName_validValues() {
    assertThat(LineNavigationTypeUtils.getDisplayName(LineNavigationType.LOXODROME)).isEqualTo("loxodrome");
    assertThat(LineNavigationTypeUtils.getDisplayName(LineNavigationType.GEODESIC)).isEqualTo("geodesic");
    assertThat(LineNavigationTypeUtils.getDisplayName(LineNavigationType.CARTESIAN)).isEqualTo("cartesian");
  }

  @ParameterizedTest
  @EnumSource(value = LineNavigationType.class, mode = EXCLUDE, names = {"LOXODROME", "GEODESIC", "CARTESIAN"})
  void getDisplayName_invalidValues_throwException(LineNavigationType navigationType) {
    assertThatThrownBy(() -> LineNavigationTypeUtils.getDisplayName(navigationType))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
