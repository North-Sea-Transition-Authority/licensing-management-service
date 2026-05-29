package uk.co.fivium.gisframework.feature;

import java.util.List;
import uk.co.fivium.grpc.gis.CoordinateSystem;

record JsonFeatures(
    List<JsonFeature> features,
    SpatialReference spatialReference
) {

  record SpatialReference(int wkid) {
    static SpatialReference from(CoordinateSystem coordinateSystem) {
      return new SpatialReference(CoordinateSystemUtils.getWkid(coordinateSystem));
    }
  }
}
