package uk.co.fivium.gisframework.feature;

import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.migration.configuration.BrokenBlockConfigurationProperties;
import uk.co.fivium.gisframework.migration.oracle.Layer;

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

  public List<Feature> getFeaturesByIds(List<UUID> ids) {
    return featureRepository.findAllById(ids);
  }

  public List<Feature> findAllByParentFeature(Feature parentFeature) {
    return featureRepository.findAllByParentFeatureId(parentFeature.getId());
  }

  public List<Feature> findAllByAttribute(String key, String value) {
    return featureRepository.findAllByAttribute(key, value);
  }

  public List<Feature> findAllByAttributeValueIn(String key, Collection<String> values) {
    return featureRepository.findAllByAttributeValueIn(key, values);
  }

  public EntityBackedFeature getEntityBackedFeature(Feature feature) {
    return new EntityBackedFeature(feature, lineService.getPolygonToLines(feature));
  }

  public List<EntityBackedFeature> getEntityBackedFeatures(Collection<Feature> features) {
    Map<Feature, Map<Polygon, List<Line>>> featureToPolygonToLines = lineService.getPolygonToLinesIn(features)
        .entrySet()
        .stream()
        .collect(Collectors.groupingBy(
            entry -> entry.getKey().getFeature(),
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

    return featureToPolygonToLines.entrySet()
        .stream()
        .map(entry -> new EntityBackedFeature(entry.getKey(), entry.getValue()))
        .toList();
  }

  public List<Feature> findAllChildFeatures() {
    return featureRepository.findAllByParentFeatureIsNotNull();
  }

  public Feature getByLegacyId(Integer legacyId) {
    return featureRepository.findByLegacyId(legacyId)
        .orElseThrow(() -> new EntityNotFoundException("Unable to find parent feature for shape %s"
            .formatted(legacyId)));
  }

  public List<Feature> findAllByLegacyIdIn(Collection<Integer> legacyIds) {
    return featureRepository.findAllByLegacyIdIn(legacyIds);
  }

  @Transactional
  public void deleteAll() {
    featureRepository.deleteAllByParentFeatureIsNotNull();
    featureRepository.deleteAll();
  }

  /**
   * This method finds all the licence blocks for a given reference block.
   * Licence blocks are linked to reference blocks based on their QUADRANT_NO and BLOCK_NO feature attributes.
   * Some licence blocks span multiple ref blocks, and therefore the attributes won't match. These need to be manually included
   * in the brokenBlockConfigurationProperties
   * @param referenceBlock The reference block Feature.
   * @param licenceBlocks A list of all the licence block features
   * @return A list of features which are all the licence blocks in the given reference block.
   */
  public List<Feature> findLicenseBlocksForRefBlock(Feature referenceBlock, Collection<Feature> licenceBlocks) {
    var brokenLicenseBlockNames = brokenBlockConfigurationProperties.getBrokenLicenseBlockNames(referenceBlock.getFeatureName());

    var quadrantNumber = String.valueOf(referenceBlock.getAttributes().get("QUADRANT_NO"));
    var blockNumber = String.valueOf(referenceBlock.getAttributes().get("BLOCK_NO"));

    return licenceBlocks
        .stream()
        .filter(block -> Layer.BLOCKS.name().equals(String.valueOf(block.getAttributes().get("LAYER"))))
        .filter(block -> (
            quadrantNumber.equals(String.valueOf(block.getAttributes().get("QUADRANT_NO")))
                && blockNumber.equals(String.valueOf(block.getAttributes().get("BLOCK_NO"))))
            || brokenLicenseBlockNames.contains(block.getFeatureName())
        )
        .toList();
  }

  public Feature getFeatureOrThrow(UUID featureId) {
    return featureRepository.findById(featureId).orElseThrow(() ->
        new EntityNotFoundException("Feature %s not found".formatted(featureId)));
  }
}
