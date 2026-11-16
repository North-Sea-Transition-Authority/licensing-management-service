package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import java.math.BigDecimal;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.grpc.gis.CoordinateSystem;

public class FeatureSummaryTestUtil {

  public static final int ED50_WKID = CoordinateSystemUtils.getWkid(CoordinateSystem.ED50);

  private FeatureSummaryTestUtil() {}

  public static FeatureSummary featureSummary(String name, BigDecimal area) {
    return new FeatureSummary(ED50_WKID, name, area, null, null);
  }

  public static FeatureSummary blockFeatureSummary(String quadrantNo, String blockNo, BigDecimal area) {
    return new FeatureSummary(ED50_WKID, "%s/%s".formatted(quadrantNo, blockNo), area, quadrantNo, blockNo);
  }
}
