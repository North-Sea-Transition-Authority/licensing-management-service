package uk.co.fivium.gisframework.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.migration.configuration.BrokenBlockConfigurationProperties;

@Profile("gis-migration")
@Service
public class MigrationValidationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationValidationService.class);
  private static final String SHAPE_TYPE_ATTRIBUTE = "SHAPE_TYPE";

  private final FeatureService featureService;
  private final GrpcClientService grpcClientService;
  private final LineService lineService;
  private final PolygonService polygonService;
  private final BrokenBlockConfigurationProperties brokenBlockConfigurationProperties;

  public MigrationValidationService(
      FeatureService featureService,
      GrpcClientService grpcClientService,
      LineService lineService, PolygonService polygonService,
      BrokenBlockConfigurationProperties brokenBlockConfigurationProperties
  ) {
    this.featureService = featureService;
    this.grpcClientService = grpcClientService;
    this.lineService = lineService;
    this.polygonService = polygonService;
    this.brokenBlockConfigurationProperties = brokenBlockConfigurationProperties;
  }

  /**
   * This method runs all the post-migration validation for blocks and subareas.
   */
  public void blockAndSubareaValidation() {
    for (var childFeature : featureService.findAllChildFeatures()) {
      var parentFeature = childFeature.getParentFeature();
      var response = grpcClientService.validateBlockAndSubarea(
          featureService.getEntityBackedFeature(childFeature),
          featureService.getEntityBackedFeature(parentFeature)
      );
      var isValid = response.getIsValid();
      if (!isValid) {
        LOGGER.error("Validation error: {} Child Feature: {} Parent Feature: {}",
            response.getMessage(),
            childFeature.getId(),
            parentFeature.getId()
        );
      } else {
        LOGGER.info("Child {} passed validation checks", childFeature.getFeatureName());
      }
    }
  }

  public void verifySubareasTopologicallyEqualToBlock() {

    for (var parent : featureService.findAllByAttribute(SHAPE_TYPE_ATTRIBUTE, "BLOCK")) {
      if (parent.getLegacyId() != 5610939) {
        continue; //TODO EPGF-18: remove this for the actual migration.
      }
      var childPolygons = polygonService.findAllByFeatureIn(featureService.findAllByParentFeature(parent));
      List<List<String>> childPolygonLines = new ArrayList<>();
      childPolygons.forEach(childPolygon ->
          childPolygonLines.add(lineService.findAllByPolygon(childPolygon).stream().map(Line::getEsriJson).toList()));

      var response = grpcClientService.validateTopologicallyEqual(
          childPolygonLines,
          featureService.getEntityBackedFeature(parent)
      );

      var isValid = response.getIsValid();
      if (!isValid) {
        LOGGER.error("Validation error: {} Feature: {}",
            response.getMessage(),
            parent.getId()
        );
      } else {
        LOGGER.info("Parent {} is topologically equal to all of its children", parent.getFeatureName());
      }
    }
  }

  /**
   * This method verifies that all license blocks are spatially contained within their reference blocks,
   * and that reference block geodesic lines overlap their license block geodesic lines.
   */
  public void validateReferenceBlocks() {
    var licenseBlocks = featureService.findAllByAttribute(SHAPE_TYPE_ATTRIBUTE, "BLOCK");
    for (var refBlock : featureService.findAllByAttribute(SHAPE_TYPE_ATTRIBUTE, "REF_BLOCK")) {
      var filteredLicenseBlocks = licenseBlocks
          .stream()
          .filter(block -> block.getFeatureName().startsWith(refBlock.getFeatureName()))
          .filter(block -> !brokenBlockConfigurationProperties.getBrokenLicenseBlockNames(refBlock.getFeatureName())
              .contains(block.getFeatureName()))
          .collect(Collectors.toSet());

      var licenseBlockFeatures = filteredLicenseBlocks.stream()
          .map(featureService::getEntityBackedFeature)
          .toList();

      var response = grpcClientService.validateReferenceBlock(
          featureService.getEntityBackedFeature(refBlock),
          licenseBlockFeatures
      );

      var isValid = response.getIsValid();
      if (!isValid) {
        LOGGER.error("Validation error: {} Reference Block: {}",
            response.getMessage(),
            refBlock.getFeatureName()
        );
      } else {
        LOGGER.info("All license blocks are contained by ref block {}", refBlock.getFeatureName());
      }
    }
  }
}