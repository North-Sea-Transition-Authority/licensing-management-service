package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;

public record FeatureSummary(
    int srsWkid,
    String name,
    BigDecimal area,
    @Nullable String quadrantNo,
    @Nullable String blockNo
) {

  public static final Comparator<FeatureSummary> BLOCK_ORDER = Comparator
      .comparing(FeatureSummary::quadrantNo, LicenceBlockFeatureUtil.PART_ORDER)
      .thenComparing(FeatureSummary::blockNo, LicenceBlockFeatureUtil.PART_ORDER);

  public static FeatureSummary from(Feature feature) {
    var attributes = Optional.ofNullable(feature.getAttributes()).orElse(Map.of());
    return new FeatureSummary(
        CoordinateSystemUtils.getWkid(feature.getCoordinateSystem()),
        feature.getFeatureName(),
        feature.getFeatureArea(),
        attributes.get(FeatureAttribute.QUADRANT_NO.name()),
        attributes.get(FeatureAttribute.BLOCK_NO.name())
    );
  }
}
