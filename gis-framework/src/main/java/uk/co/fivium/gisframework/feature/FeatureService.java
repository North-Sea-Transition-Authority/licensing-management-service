package uk.co.fivium.gisframework.feature;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureService {

  private final FeatureRepository featureRepository;

  public FeatureService(FeatureRepository featureRepository) {
    this.featureRepository = featureRepository;
  }

  @Transactional
  public void saveFeature(Feature feature) {
    featureRepository.save(feature);
  }

  public List<Feature> findAllByParentFeature(Feature parentFeature) {
    return featureRepository.findAllByParentFeatureId(parentFeature.getId());
  }
}
