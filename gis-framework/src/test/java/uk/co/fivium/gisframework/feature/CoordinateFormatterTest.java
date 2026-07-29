package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.fivium.grpc.gis.CoordinateSystem;

class CoordinateFormatterTest {

  @Test
  void formatCoordinate_whenBritishNationalGrid_thenSingleOsGridReference() {
    assertThat(CoordinateFormatter.formatCoordinate(CoordinateSystem.BRITISH_NATIONAL_GRID, 651409.4, 313177.6))
        .isEqualTo("TG 5140 1317");
  }

  @ParameterizedTest
  @EnumSource(value = CoordinateSystem.class, names = {"ED50", "WGS84", "ETRS89"})
  void formatCoordinate_whenGeographicDatum_thenLatitudeThenLongitude(CoordinateSystem coordinateSystem) {
    assertThat(CoordinateFormatter.formatCoordinate(coordinateSystem, 2.5, 53.5))
        .isEqualTo("53°30′00.000″N 2°30′00.000″E");
  }

  @ParameterizedTest
  @EnumSource(value = CoordinateSystem.class, mode = EXCLUDE, names = {"ED50", "BRITISH_NATIONAL_GRID", "WGS84", "ETRS89"})
  void formatCoordinate_whenUnknownCoordinateSystem_thenThrowException(CoordinateSystem coordinateSystem) {
    assertThatThrownBy(() -> CoordinateFormatter.formatCoordinate(coordinateSystem, 1.0, 2.0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
