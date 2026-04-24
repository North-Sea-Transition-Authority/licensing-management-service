package uk.co.fivium.gisframework.migration;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@Profile("gis-migration")
@Service
public class MigrationValidationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationValidationService.class);

  private final FeatureService featureService;
  private final GrpcClientService grpcClientService;
  private final LineService lineService;
  private final PolygonService polygonService;

  public MigrationValidationService(
      FeatureService featureService,
      GrpcClientService grpcClientService,
      LineService lineService, PolygonService polygonService) {
    this.featureService = featureService;
    this.grpcClientService = grpcClientService;
    this.lineService = lineService;
    this.polygonService = polygonService;
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

    for (var parent : featureService.findAllByAttribute("SHAPE_TYPE", "BLOCK")) {
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
}