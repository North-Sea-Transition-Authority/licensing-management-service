package uk.co.fivium.gisframework.operator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Polygon;

@Service
public class StrataSplitOperatorService {

  private final FeatureService featureService;
  private final OperatorResultProcessingService operatorResultProcessingService;

  public StrataSplitOperatorService(
      FeatureService featureService,
      OperatorResultProcessingService operatorResultProcessingService
  ) {
    this.featureService = featureService;
    this.operatorResultProcessingService = operatorResultProcessingService;
  }

  /**
   * Splits the target at the strata depth. Each polygon spanning the depth becomes an upper polygon ending at the depth
   * and a lower polygon starting at it; polygons wholly above or below the depth are copied unchanged to that side.
   *
   * @return either empty if no polygon spans the depth, or the feature above the depth followed by the feature below.
   */
  @Transactional
  public List<Feature> strataSplitPolygon(Feature target, long strataDepth) {
    var entityBackedFeature = featureService.getEntityBackedFeature(target);

    Map<Polygon, Polygon> aboveStrataPolygonsToSource = new HashMap<>();
    Map<Polygon, Polygon> belowStrataPolygonsToSource = new HashMap<>();
    boolean anySplit = false;

    for (var polygon : entityBackedFeature.polygonToLines().keySet()) {
      if (canSplitAt(polygon, strataDepth)) {
        anySplit = true;
        aboveStrataPolygonsToSource.put(copyPolygonWithDepths(polygon, polygon.getStartDepth(), strataDepth), polygon);
        belowStrataPolygonsToSource.put(copyPolygonWithDepths(polygon, strataDepth, polygon.getEndDepth()), polygon);
      } else {
        var untouchedPolygonToSource = polygon.getEffectiveEndDepth() >= strataDepth
            ? aboveStrataPolygonsToSource
            : belowStrataPolygonsToSource;
        untouchedPolygonToSource.put(copyPolygon(polygon), polygon);
      }
    }

    if (!anySplit) {
      return List.of();
    }

    return List.of(
        operatorResultProcessingService.processStrataSplitFeature(
            entityBackedFeature,
            aboveStrataPolygonsToSource,
            "above"
        ),
        operatorResultProcessingService.processStrataSplitFeature(
            entityBackedFeature,
            belowStrataPolygonsToSource,
            "below"
        )
    );
  }

  private boolean canSplitAt(Polygon polygon, long depth) {
    return polygon.coversDepth(depth) && polygon.getEffectiveStartDepth() != depth;
  }

  private Polygon copyPolygon(Polygon polygon) {
    return copyPolygonWithDepths(polygon, polygon.getStartDepth(), polygon.getEndDepth());
  }

  private Polygon copyPolygonWithDepths(Polygon source, Long startDepth, Long endDepth) {
    var polygon = new Polygon();
    polygon.setAttributes(new HashMap<>(source.getAttributes()));
    polygon.setStartDepth(startDepth);
    polygon.setEndDepth(endDepth);
    return polygon;
  }
}
