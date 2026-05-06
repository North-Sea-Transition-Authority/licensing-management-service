package uk.co.fivium.gisframework.operator;

import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;

@Service
class OperatorResultProcessingService {

  @Transactional
  public Feature processOutputPolygon(List<Feature> inputFeatures,
                                      String outputEsriJsonPolygon) {
    return null; //TODO EPGF-54-3 Port post-processing logic from gis-alphatest
  }
}
