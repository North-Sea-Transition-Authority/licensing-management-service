package uk.co.fivium.gisframework.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@Service
public class PolygonService {

  private final PolygonRepository polygonRepository;
  private final FeatureService featureService;
  private final GrpcClientService grpcClientService;

  public PolygonService(PolygonRepository polygonRepository,
                        FeatureService featureService,
                        GrpcClientService grpcClientService) {
    this.polygonRepository = polygonRepository;
    this.featureService = featureService;
    this.grpcClientService = grpcClientService;
  }

  @Transactional
  public void savePolygon(Polygon polygon) {
    polygonRepository.save(polygon);
  }

  public List<Polygon> findAllByFeatureIn(Collection<Feature> features) {
    return polygonRepository.findAllByFeatureIn(features);
  }

  /**
   * Generates all the EsriJSON polygons for a given feature.
   *
   * @param feature the feature whose polygons will be built as EsriJSON.
   * @return a list of EsriJSON polygons for the given feature
   */
  public List<String> getPolygonsAsEsriJson(Feature feature) {
    var entityBackedFeature = featureService.getEntityBackedFeature(feature);
    return getPolygonsAsEsriJson(entityBackedFeature);
  }

  private List<String> getPolygonsAsEsriJson(EntityBackedFeature entityBackedFeature) {
    List<String> polygonsAsEsriJson = new ArrayList<>();

    for (var polygonToLines : entityBackedFeature.polygonToLines().entrySet()) {
      var lineEsriJsons = polygonToLines.getValue()
          .stream()
          .sorted(Comparator.comparing(Line::getRingConnectionOrder))
          .map(Line::getEsriJson)
          .toList();
      String polygonEsriJson = grpcClientService.buildPolygon(
          lineEsriJsons,
          entityBackedFeature.feature().getCoordinateSystem()
      );
      polygonsAsEsriJson.add(polygonEsriJson);
    }

    return polygonsAsEsriJson;
  }
}
