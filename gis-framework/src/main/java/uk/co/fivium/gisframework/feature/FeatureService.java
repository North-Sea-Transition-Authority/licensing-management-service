package uk.co.fivium.gisframework.feature;

import java.util.HashMap;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureService {

  private final FeatureRepository featureRepository;
  private final PolygonRepository polygonRepository;
  private final LineRepository lineRepository;

  public FeatureService(FeatureRepository featureRepository,
                        PolygonRepository polygonRepository,
                        LineRepository lineRepository
  ) {
    this.featureRepository = featureRepository;
    this.polygonRepository = polygonRepository;
    this.lineRepository = lineRepository;
  }

  @Transactional
  public void saveFeature(Feature feature) {
    featureRepository.save(feature);
  }

  public List<Feature> findAllByParentFeature(Feature parentFeature) {
    return featureRepository.findAllByParentFeatureId(parentFeature.getId());
  }

  public EntityBackedFeature getEntityBackedFeature(Feature feature) {
    var polygons = polygonRepository.findAllByFeature(feature);

    Map<Polygon, List<Line>> polygonToLines = new HashMap<>();

    for (var polygon : polygons) {
      polygonToLines.put(polygon, lineRepository.findAllByPolygon(polygon));
    }

    return
        new EntityBackedFeature(
            feature,
            polygonToLines
        );
  }

  public Feature getByLegacyId(Integer legacyId) {
    return featureRepository.findByLegacyId(legacyId)
        .orElseThrow(() -> new EntityNotFoundException("Unable to find parent feature for shape %s"
            .formatted(legacyId)));
  }

  @Transactional
  public void deleteAll() {
    featureRepository.deleteAllByParentFeatureIsNotNull();
    featureRepository.deleteAll();
  }
}
