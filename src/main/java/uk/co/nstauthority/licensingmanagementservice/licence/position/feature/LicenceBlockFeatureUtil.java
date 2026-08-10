package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.Layer;


public final class LicenceBlockFeatureUtil {

  private static final Comparator<String> PART_ORDER = Comparator
      .comparing(LicenceBlockFeatureUtil::numericValue, Comparator.nullsLast(Comparator.naturalOrder()))
      .thenComparing(Comparator.nullsLast(Comparator.naturalOrder()));

  /**
   * Feature's store quadrant number and block number as strings in their attribute map. This comparator converts them to numbers
   * and compares the quadrant number and then the block number.
   */
  public static final Comparator<Feature> BLOCK_ORDER = Comparator
      .comparing(
          (Feature feature) ->
              feature.getAttributes().getOrDefault(FeatureAttribute.QUADRANT_NO.name(), null),
          PART_ORDER
      )
      .thenComparing(feature ->
              feature.getAttributes().getOrDefault(FeatureAttribute.BLOCK_NO.name(), null),
          PART_ORDER
      );

  private LicenceBlockFeatureUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static boolean isLicenceBlock(Feature feature) {
    return Layer.BLOCKS.name().equals(
        Optional.ofNullable(feature.getAttributes()).orElse(Map.of()).get(FeatureAttribute.LAYER.name())
    );
  }

  private static Integer numericValue(String value) {
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException expected) {
      return null;
    }
  }
}
