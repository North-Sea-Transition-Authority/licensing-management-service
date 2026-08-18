package uk.co.fivium.gisframework.feature;

import java.util.List;
import uk.co.fivium.grpc.gis.CoordinateSystem;

/**
 * Represents a collection of features in JSON format, along with their spatial reference information in the format
 * that is expected by the frontend OpenLayers library.
 * @param features The features in JSON format.
 * @param spatialReference The spatial reference information. This should match the coordinate system of the features.
 */
public record JsonFeatures(
    List<JsonFeature> features,
    SpatialReference spatialReference
) {

  public record SpatialReference(int wkid) {
    public static SpatialReference from(CoordinateSystem coordinateSystem) {
      return new SpatialReference(CoordinateSystemUtils.getWkid(coordinateSystem));
    }
  }
}
