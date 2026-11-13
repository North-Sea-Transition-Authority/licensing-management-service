package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.Layer;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

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

  public static Map<String, String> toBlockCheckboxOptions(List<Feature> blockFeatures) {
    return blockFeatures.stream()
        .collect(StreamUtil.toLinkedHashMap(
            feature -> feature.getId().toString(),
            feature -> "Block %s".formatted(feature.getFeatureName())));
  }

  public static boolean isLicenceBlock(Feature feature) {
    return Layer.BLOCKS.name().equals(
        Optional.ofNullable(feature.getAttributes()).orElse(Map.of()).get(FeatureAttribute.LAYER.name())
    );
  }

  public static BigDecimal getTotalArea(Collection<Feature> features) {
    return features.stream()
        .map(Feature::getFeatureArea)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  // feature areas are stored in square metres
  public static String formatArea(BigDecimal squareMetres) {
    var squareKilometres = squareMetres.movePointLeft(6).setScale(2, RoundingMode.HALF_UP);
    return String.format(Locale.UK, "%,.2f km²", squareKilometres);
  }

  private static Integer numericValue(String value) {
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException expected) {
      return null;
    }
  }
}
