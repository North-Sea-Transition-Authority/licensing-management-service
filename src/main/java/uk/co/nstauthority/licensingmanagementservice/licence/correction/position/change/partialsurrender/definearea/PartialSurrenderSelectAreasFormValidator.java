package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.relationship.BoundingBox;
import uk.co.fivium.gisframework.relationship.FeatureRelationshipService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PartialSurrenderOperationRule;

@Service
public class PartialSurrenderSelectAreasFormValidator {

  private static final String SURRENDERED_FEATURE_IDS_FIELD = "surrenderedFeatureIds";

  private final FeatureRelationshipService featureRelationshipService;

  PartialSurrenderSelectAreasFormValidator(FeatureRelationshipService featureRelationshipService) {
    this.featureRelationshipService = featureRelationshipService;
  }

  public boolean hasSelectionErrors(
      PartialSurrenderSelectAreasForm form,
      Errors errors,
      List<Feature> activeFeatures,
      Feature blockFeature
  ) {
    var featureIds = getFeatureIds(activeFeatures);
    var selected = form.getSurrenderedFeatureIds();

    if (isSelectionEmptyOrIncludesInactiveAreas(selected, featureIds)) {
      errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.required",
          PartialSurrenderOperationRule.AREAS_REQUIRED.getMessage()
      );
    } else if (isSelectionOfEveryActiveArea(selected, featureIds)) {
      errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.invalid",
          PartialSurrenderOperationRule.CANNOT_SURRENDER_ALL_AREAS.getMessage()
      );
    } else if (featureRelationshipService.is2dTopologicallyEqual(
        activeFeatures.stream().filter(feature -> selected.contains(feature.getId())).toList(),
        blockFeature
    )) {
      errors.rejectValue(
          SURRENDERED_FEATURE_IDS_FIELD,
          "surrenderedFeatureIds.wholeBlock",
          PartialSurrenderOperationRule.CANNOT_SURRENDER_WHOLE_BLOCK.getMessage()
      );
    }

    return errors.hasErrors();
  }

  /**
   * Checks the areas kept by a selection for overlap with other blocks. Only call this for a selection without
   * {@link #hasSelectionErrors(PartialSurrenderSelectAreasForm, Errors, List, Feature) selection errors}.
   *
   * @throws IllegalStateException if no areas are selected, a selected area is not active, or every area is selected.
   */
  public boolean hasOverlapErrors(
      PartialSurrenderSelectAreasForm form,
      Errors errors,
      List<Feature> activeFeatures,
      Map<Line, BoundingBox> otherActiveBlockLines
  ) {
    var featureIds = getFeatureIds(activeFeatures);
    var selected = form.getSurrenderedFeatureIds();
    if (isSelectionEmptyOrIncludesInactiveAreas(selected, featureIds)
        || isSelectionOfEveryActiveArea(selected, featureIds)) {
      throw new IllegalStateException("Areas to surrender must be some but not all of the active areas");
    }

    var keptAreas = activeFeatures.stream().filter(feature -> !selected.contains(feature.getId())).toList();
    getOverlapErrors(keptAreas, otherActiveBlockLines).forEach(message -> errors.rejectValue(
        SURRENDERED_FEATURE_IDS_FIELD,
        "surrenderedFeatureIds.overlap",
        message
    ));

    return errors.hasErrors();
  }

  /**
   * Checks kept areas for overlap with other blocks by finding the blocks with a line that crosses a kept area. Blocks
   * should never lie fully inside one another, and a block whose lines only run between points on a kept area's edges
   * is not treated as overlapping it.
   */
  public List<String> getOverlapErrors(
      Collection<Feature> keptAreas,
      Map<Line, BoundingBox> otherActiveBlockLines
  ) {
    return keptAreas.stream()
        .collect(Collectors.groupingBy(Feature::getCoordinateSystem))
        .entrySet()
        .stream()
        .flatMap(entry -> featureRelationshipService
            .get2dCrossedFeatures(
                entry.getValue(),
                getLinesInCoordinateSystem(otherActiveBlockLines, entry.getKey())
            )
            .stream())
        .distinct()
        .map(overlappedBlock -> PartialSurrenderOperationRule.KEPT_AREA_OVERLAPS_BLOCK
            .getMessage(overlappedBlock.getFeatureName()))
        .toList();
  }

  private static Map<Line, BoundingBox> getLinesInCoordinateSystem(
      Map<Line, BoundingBox> lineBoundingBoxes,
      CoordinateSystem coordinateSystem
  ) {
    return lineBoundingBoxes.entrySet()
        .stream()
        .filter(entry -> entry.getKey().getPolygon().getFeature().getCoordinateSystem() == coordinateSystem)
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (boundingBox, duplicateBoundingBox) -> boundingBox,
            LinkedHashMap::new
        ));
  }

  private static Set<UUID> getFeatureIds(List<Feature> features) {
    return features.stream()
        .map(Feature::getId)
        .collect(Collectors.toSet());
  }

  private static boolean isSelectionEmptyOrIncludesInactiveAreas(Set<UUID> selected, Set<UUID> activeFeatureIds) {
    return CollectionUtils.isEmpty(selected) || !activeFeatureIds.containsAll(selected);
  }

  private static boolean isSelectionOfEveryActiveArea(Set<UUID> selected, Set<UUID> activeFeatureIds) {
    return selected.size() >= activeFeatureIds.size();
  }
}
