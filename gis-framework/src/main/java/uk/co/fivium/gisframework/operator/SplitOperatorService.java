package uk.co.fivium.gisframework.operator;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@Service
public class SplitOperatorService {

  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final OperatorResultProcessingService operatorResultProcessingService;

  public SplitOperatorService(PolygonService polygonService,
                              GrpcClientService grpcClientService,
                              OperatorResultProcessingService operatorResultProcessingService) {
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.operatorResultProcessingService = operatorResultProcessingService;
  }


  @Transactional
  public List<Feature> splitPolygon(Feature target,
                                    String cutterLineEsriJson) {
    List<String> esriJsonPolygons = polygonService.getPolygonsAsEsriJson(target);
    List<String> resultEsriJsonPolygons = new ArrayList<>();

    esriJsonPolygons.forEach(polygon ->
        resultEsriJsonPolygons.addAll(grpcClientService.splitPolygon(polygon, cutterLineEsriJson))
    );

    List<Feature> resultFeatures = new ArrayList<>();

    for (int i = 0; i < resultEsriJsonPolygons.size(); i++) {
      var polygon = resultEsriJsonPolygons.get(i);
      var newFeature = operatorResultProcessingService.processOutputPolygon(List.of(target), polygon, i + 1);
      resultFeatures.add(newFeature);
    }

    return resultFeatures;
  }
}
