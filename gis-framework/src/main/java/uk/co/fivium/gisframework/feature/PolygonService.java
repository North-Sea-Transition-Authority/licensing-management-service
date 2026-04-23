package uk.co.fivium.gisframework.feature;

import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolygonService {

  private final PolygonRepository polygonRepository;

  public PolygonService(PolygonRepository polygonRepository) {
    this.polygonRepository = polygonRepository;
  }

  @Transactional
  public void savePolygon(Polygon polygon) {
    polygonRepository.save(polygon);
  }

  public List<Polygon> findAllByFeatureIn(Collection<Feature> features) {
    return polygonRepository.findAllByFeatureIn(features);
  }
}
