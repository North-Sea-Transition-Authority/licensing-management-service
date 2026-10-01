package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.operator.CropOperatorService;
import uk.co.fivium.gisframework.operator.CropResultDto;
import uk.co.fivium.grpc.gis.IntersectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@Service
class PartialSurrenderSubareaService {

  private final LicencePositionSpatialService licencePositionSpatialService;
  private final FeatureService featureService;
  private final CropOperatorService cropOperatorService;

  PartialSurrenderSubareaService(
      LicencePositionSpatialService licencePositionSpatialService,
      FeatureService featureService,
      CropOperatorService cropOperatorService
  ) {
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.featureService = featureService;
    this.cropOperatorService = cropOperatorService;
  }

  /**
   * Works out what surrendering a block does to its subareas, relative to each part of the block kept. A subarea left
   * wholly on a part is kept on it, one left partly on it is cropped back to it as a new subarea, and one left off it
   * is relinquished from it; the original subareas are left untouched. A fully surrendered block keeps no part, so
   * records nothing. Subareas carried across from PEARS without a shape can't be cropped, so are kept as they are on
   * the first part kept.
   *
   * @param retainedFeatureIds The features holding the ground the block keeps.
   * @param previousDetails The block's previous surrender details when its surrender is unchanged. Their outcomes are
   *                        reused if they were worked out from the same subareas now going into the block.
   * @return The surrender details with the block's subarea outcomes recorded against each part kept.
   */
  @Transactional
  public SurrenderDetails processSubareas(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      @Nullable String changeId,
      UUID blockFeatureId,
      SurrenderDetails surrenderDetails,
      Collection<UUID> retainedFeatureIds,
      @Nullable SurrenderDetails previousDetails
  ) {
    if (surrenderDetails.surrenderedFeatureIds().isEmpty()
        || surrenderDetails.type() == BlockSurrenderType.FULL_SURRENDER
        || retainedFeatureIds.isEmpty()) {
      return surrenderDetails.withSubareas(Map.of());
    }

    var subareas = licencePositionSpatialService
        .getSubareasGoingIntoChange(licenceCorrection, licencePositionId, blockFeatureId, changeId);

    if (previousDetails != null && Set.copyOf(subareas).equals(inputSubareasOf(previousDetails))) {
      return surrenderDetails.withSubareas(previousDetails.retainedFeatureIdToSubareas());
    }

    var subareaIdToSubarea = subareas.stream()
        .filter(subarea -> subarea.featureId() != null)
        .collect(Collectors.toMap(
            SubareaDetails::featureId, Function.identity(), (first, duplicate) -> first, LinkedHashMap::new));
    var subareaFeatures = subareaIdToSubarea.isEmpty()
        ? List.<Feature>of()
        : featureService.getFeaturesByIds(subareaIdToSubarea.keySet());
    var retainedFeatures = subareaFeatures.isEmpty()
        ? List.<Feature>of()
        : featureService.getFeaturesByIds(retainedFeatureIds);

    var retainedFeatureIdToSubareas = new LinkedHashMap<UUID, List<SubareaSurrenderOutcome>>();
    retainedFeatures.forEach(retainedFeature -> retainedFeatureIdToSubareas.put(
        retainedFeature.getId(), cropSubareas(retainedFeature, subareaIdToSubarea, subareaFeatures)));

    var shapelessSubareas = subareas.stream()
        .filter(subarea -> subarea.featureId() == null)
        .map(SubareaSurrenderOutcome::kept)
        .toList();
    if (!shapelessSubareas.isEmpty()) {
      retainedFeatureIdToSubareas.merge(
          retainedFeatureIds.iterator().next(),
          shapelessSubareas,
          (croppedSubareas, keptSubareas) -> Stream.concat(croppedSubareas.stream(), keptSubareas.stream()).toList());
    }

    return surrenderDetails.withSubareas(retainedFeatureIdToSubareas);
  }

  private static Set<SubareaDetails> inputSubareasOf(SurrenderDetails surrenderDetails) {
    return surrenderDetails.retainedFeatureIdToSubareas().values().stream()
        .flatMap(List::stream)
        .map(SubareaSurrenderOutcome::subarea)
        .collect(Collectors.toSet());
  }

  private List<SubareaSurrenderOutcome> cropSubareas(
      Feature retainedFeature,
      Map<UUID, SubareaDetails> subareaIdToSubarea,
      List<Feature> subareaFeatures
  ) {
    return cropOperatorService.cropFeaturesToBoundary(retainedFeature, subareaFeatures).stream()
        .map(cropResult -> toSubareaOutcome(subareaIdToSubarea.get(cropResult.inputFeatureId()), cropResult))
        .toList();
  }

  private static SubareaSurrenderOutcome toSubareaOutcome(
      SubareaDetails subarea,
      CropResultDto cropResult
  ) {
    var croppedSubarea = cropResult.status() == IntersectionStatus.CROPPED
        ? new SubareaDetails(Objects.requireNonNull(cropResult.croppedFeatureId()), subarea.name(), subarea.shortName())
        : null;
    return new SubareaSurrenderOutcome(subarea, cropResult.status(), croppedSubarea);
  }
}
