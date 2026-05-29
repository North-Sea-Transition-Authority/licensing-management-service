package uk.co.fivium.gisframework.feature;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.migration.configuration.BrokenBlockConfigurationProperties;

@Service
public class FeatureService {

  private final FeatureRepository featureRepository;
  private final LineService lineService;
  private final BrokenBlockConfigurationProperties brokenBlockConfigurationProperties;

  public FeatureService(
      FeatureRepository featureRepository,
      LineService lineService,
      BrokenBlockConfigurationProperties brokenBlockConfigurationProperties) {
    this.featureRepository = featureRepository;
    this.lineService = lineService;
    this.brokenBlockConfigurationProperties = brokenBlockConfigurationProperties;
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

  /**
   * This method finds all the license blocks for a given reference block.
   * License blocks are linked to references blocks based on their name. A reference block would be called something like "11/24",
   * and a license block would be called "11/24a". There are additionally some license blocks which span multiple references
   * blocks. These special license blocks are defined in the configuration properties.
   * @param refBlockName The name of the reference block.
   * @return A list of features which are all the license block in the given reference block.
   */
  public List<Feature> findLicenseBlocksForRefBlock(String refBlockName) {
    return findAllByAttribute("SHAPE_TYPE", "BLOCK")
        .stream()
        .filter(licenseBlock ->
            licenseBlock.getFeatureName().startsWith(refBlockName)
                || brokenBlockConfigurationProperties.getBrokenLicenseBlockNames(refBlockName)
                .contains(licenseBlock.getFeatureName())
        )
        .toList();
  }

  public Feature getFeatureOrThrow(UUID featureId) {
    return featureRepository.findById(featureId).orElseThrow(() ->
        new EntityNotFoundException("Feature %s not found".formatted(featureId)));
  }
}
