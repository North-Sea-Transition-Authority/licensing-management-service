package uk.co.fivium.gisframework.migration;

import static uk.co.fivium.gisframework.migration.oracle.Layer.REFERENCE_BLOCK_LAYERS;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.migration.configuration.BrokenBlockConfigurationProperties;
import uk.co.fivium.gisframework.migration.oracle.Layer;

@Profile("gis-migration")
@Service
public class MigrationValidationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationValidationService.class);
  private static final String LAYER_ATTRIBUTE = "LAYER";

  private final FeatureService featureService;
  private final GrpcClientService grpcClientService;
  private final BrokenBlockConfigurationProperties brokenBlockConfigurationProperties;

  public MigrationValidationService(
      FeatureService featureService,
      GrpcClientService grpcClientService,
      BrokenBlockConfigurationProperties brokenBlockConfigurationProperties
  ) {
    this.featureService = featureService;
    this.grpcClientService = grpcClientService;
    this.brokenBlockConfigurationProperties = brokenBlockConfigurationProperties;
  }

  /**
   * This validates that the child polygon is contained by the parent polygon,
   * and that the geodesic lines of the child overlap with the geodesic lines of the parent, if any.
   * The parent/child combo could be a licence block and a subarea or an older licence block, and its newer version.
   */
  public void childAndParentValidation(Layer childFeatureLayer) {
    Map<Feature, List<Feature>> parentToChildFeatures = featureService.findAllChildFeatures()
        .stream()
        .filter(child -> {
          var layer = child.getAttributes().get(LAYER_ATTRIBUTE);
          return childFeatureLayer.name().equals(layer);
        })
        .collect(Collectors.groupingBy(Feature::getParentFeature));

    Map<UUID, EntityBackedFeature> entityBackedParentsById = featureService.getEntityBackedFeatures(
            parentToChildFeatures.keySet()
        )
        .stream()
        .collect(Collectors.toMap(parent -> parent.feature().getId(), Function.identity()));

    for (var entry : parentToChildFeatures.entrySet()) {
      var parentFeature = entityBackedParentsById.get(entry.getKey().getId());

      for (var childFeature : featureService.getEntityBackedFeatures(entry.getValue())) {
        var response = grpcClientService.validateBlockAndSubarea(childFeature, parentFeature);
        if (!response.getIsValid()) {
          LOGGER.error("Validation error: {} Child Feature: {} Parent Feature: {}",
              response.getMessage(),
              childFeature.feature().getLegacyId(),
              entry.getKey().getLegacyId()
          );
        } else {
          LOGGER.info("Child {} passed validation checks", childFeature.feature().getLegacyId());
        }
      }
    }
  }

  /**
   * Validates that subareas are topologically equal to their associated license block.
   */
  public void verifySubareasTopologicallyEqualToBlock() {
    Map<UUID, List<Feature>> subareasByParentId = featureService.findAllByAttribute(LAYER_ATTRIBUTE, Layer.SUBAREAS.name())
        .stream()
        .filter(subarea -> subarea.getParentFeature() != null)
        .collect(Collectors.groupingBy(subarea -> subarea.getParentFeature().getId()));

    var entityBackedBlocks = featureService.getEntityBackedFeatures(
        featureService.findAllByAttribute(LAYER_ATTRIBUTE, Layer.BLOCKS.name())
    );

    for (var entityBackedBlock : entityBackedBlocks) {
      var subareas = featureService.getEntityBackedFeatures(
          subareasByParentId.getOrDefault(entityBackedBlock.feature().getId(), List.of())
      );

      if (subareas.isEmpty()) {
        LOGGER.warn("Parent {} has no subareas", entityBackedBlock.feature().getLegacyId());
        continue;
      }

      var childPolygonLines = subareas
          .stream()
          .flatMap(subarea -> subarea.polygonToLines().values().stream())
          .map(lines -> lines.stream().map(Line::getEsriJson).toList())
          .toList();
      var response = grpcClientService.validateTopologicallyEqual(
          childPolygonLines,
          entityBackedBlock
      );

      if (!response.getIsValid()) {
        LOGGER.error("Validation error: {} Feature: {}",
            response.getMessage(),
            entityBackedBlock.feature().getLegacyId()
        );
      } else {
        LOGGER.info("Parent {} is topologically equal to all of its children", entityBackedBlock.feature().getLegacyId());
      }
    }
  }

  /**
   * This method verifies that all license blocks are spatially contained within their reference blocks,
   * and that reference block geodesic lines overlap their license block geodesic lines.
   */
  public void validateReferenceBlocks() {
    var licenseBlocks = featureService.findAllByAttribute(LAYER_ATTRIBUTE, Layer.BLOCKS.name());
    var refBlockLayers = REFERENCE_BLOCK_LAYERS.stream().map(Layer::name).toList();
    var entityBackedRefBlocks = featureService.getEntityBackedFeatures(
        featureService.findAllByAttributeValueIn(LAYER_ATTRIBUTE, refBlockLayers)
    );

    for (var entityBackedRefBlock : entityBackedRefBlocks) {
      var refBlockName = entityBackedRefBlock.feature().getFeatureName();
      var brokenLicenseBlockNames = brokenBlockConfigurationProperties.getBrokenLicenseBlockNames(refBlockName);

      var filteredLicenseBlocks = licenseBlocks
          .stream()
          .filter(block -> block.getFeatureName().startsWith(refBlockName))
          .filter(block -> !brokenLicenseBlockNames.contains(block.getFeatureName()))
          .toList();

      var response = grpcClientService.validateReferenceBlock(
          entityBackedRefBlock,
          featureService.getEntityBackedFeatures(filteredLicenseBlocks)
      );

      if (!response.getIsValid()) {
        LOGGER.error("Validation error: {} Reference Block: {}",
            response.getMessage(),
            entityBackedRefBlock.feature().getLegacyId()
        );
      } else {
        LOGGER.info("All license blocks are contained by ref block {}", entityBackedRefBlock.feature().getLegacyId());
      }
    }
  }
}