package uk.co.fivium.gisframework.feature;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureService {

  private final FeatureRepository featureRepository;
  private final LineService lineService;

  public FeatureService(
      FeatureRepository featureRepository,
      LineService lineService
  ) {
    this.featureRepository = featureRepository;
    this.lineService = lineService;
  }

  @Transactional
  public void saveFeature(Feature feature) {
    featureRepository.save(feature);
  }

  public List<Feature> findAllByParentFeature(Feature parentFeature) {
    return featureRepository.findAllByParentFeatureId(parentFeature.getId());
  }

  public List<Feature> findAllByAttribute(String key, String value) {
    return featureRepository.findAllByAttribute(key, value);
  }

  public EntityBackedFeature getEntityBackedFeature(Feature feature) {
    return new EntityBackedFeature(feature, lineService.getPolygonToLines(feature));
  }

  public List<Feature> findAllChildFeatures() {
    return featureRepository.findAllByParentFeatureIsNotNull();
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
