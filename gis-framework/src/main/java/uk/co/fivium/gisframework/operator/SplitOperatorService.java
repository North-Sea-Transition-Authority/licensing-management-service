package uk.co.fivium.gisframework.operator;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@Service
public class SplitOperatorService {

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final OperatorResultProcessingService operatorResultProcessingService;

  public SplitOperatorService(
      FeatureService featureService,
      PolygonService polygonService,
      GrpcClientService grpcClientService,
      OperatorResultProcessingService operatorResultProcessingService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.operatorResultProcessingService = operatorResultProcessingService;
  }

  /**
   * Splits each of the target's polygons that cover the depth (or all of them if no depth is given) with the cutter
   * line. A polygon occupies its depth range as a prism, so one 2D cut applies to every depth it spans. Polygons the
   * line doesn't fully cross, or that don't cover the depth, are kept as they are.
   *
   * @return one new feature per cut piece and per uncut polygon, each keeping the attributes and depths of the
   *         polygon it came from, or an empty list if no polygon was cut.
   */
  @Transactional
  public List<Feature> splitPolygon(Feature target, String cutterLineEsriJson, Long lineDepth) {
    var entityBackedFeature = featureService.getEntityBackedFeature(target);
    var esriJsonByPolygonId = polygonService.getPolygonIdToEsriJson(entityBackedFeature);
    List<EsriJsonPolygonWithSources> resultEsriJsonPolygonsWithSources = new ArrayList<>();

    boolean anyCuts = false;
    for (var polygon : entityBackedFeature.polygonToLines().keySet()) {
      var polygonEsriJson = esriJsonByPolygonId.get(polygon.getId());
      List<String> result = List.of();
      if (lineDepth == null || polygon.coversDepth(lineDepth)) {
        result = grpcClientService.splitPolygon(polygonEsriJson, cutterLineEsriJson);
      }
      if (result.isEmpty()) {
        resultEsriJsonPolygonsWithSources.add(new EsriJsonPolygonWithSources(polygonEsriJson, List.of(polygon)));
      } else {
        anyCuts = true;
        result.forEach(esriJson -> resultEsriJsonPolygonsWithSources.add(
            new EsriJsonPolygonWithSources(esriJson, List.of(polygon))
        ));
      }
    }

    if (!anyCuts) {
      return List.of();
    }

    List<Feature> resultFeatures = new ArrayList<>();
    int featureNameSuffix = 1;
    for (var resultEsriJsonPolygonWithSources : resultEsriJsonPolygonsWithSources) {
      resultFeatures.add(operatorResultProcessingService.processOutputPolygon(
          target,
          resultEsriJsonPolygonWithSources,
          featureNameSuffix++
      ));
    }

    return resultFeatures;
  }
}
