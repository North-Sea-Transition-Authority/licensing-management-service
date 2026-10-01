package uk.co.nstauthority.licensingmanagementservice.licence.position.spatial;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil.BLOCK_ORDER;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;

@Service
public class LicencePositionSpatialService {

  private final LicencePositionViewService licencePositionViewService;
  private final FeatureService featureService;

  public LicencePositionSpatialService(
      LicencePositionViewService licencePositionViewService,
      FeatureService featureService
  ) {
    this.licencePositionViewService = licencePositionViewService;
    this.featureService = featureService;
  }

  public List<Feature> getBlockFeaturesGoingIntoChange(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      @Nullable String changeId
  ) {
    return getBlockFeaturesGoingIntoChange(licenceCorrection, licencePosition.getId(), changeId);
  }

  public List<Feature> getBlockFeaturesGoingIntoChange(
      LicencePositionCorrection licencePositionCorrection,
      @Nullable String changeId
  ) {
    return getBlockFeaturesGoingIntoChange(
        licencePositionCorrection.getLicenceCorrection(),
        licencePositionCorrection.getPositionId(),
        changeId
    );
  }

  public List<Feature> getBlockFeaturesGoingIntoChange(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      @Nullable String changeId
  ) {
    return toBlockFeatures(
        getSpatialStateGoingIntoChange(licenceCorrection, licencePositionId, changeId).blockFeatureIds()
    );
  }

  public List<SubareaDetails> getSubareasGoingIntoChange(
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId,
      @Nullable String changeId
  ) {
    return getSubareasGoingIntoChange(
        licencePositionCorrection.getLicenceCorrection(),
        licencePositionCorrection.getPositionId(),
        blockFeatureId,
        changeId
    );
  }

  /**
   * The subareas dividing up a block going into a change. A block kept in part by a partial surrender hands its
   * subareas on to the features it was cut back to, each taking the subareas that lie on it.
   */
  public List<SubareaDetails> getSubareasGoingIntoChange(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      UUID blockFeatureId,
      @Nullable String changeId
  ) {
    return getSpatialStateGoingIntoChange(licenceCorrection, licencePositionId, changeId).subareasOf(blockFeatureId);
  }

  private SpatialState getSpatialStateGoingIntoChange(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      @Nullable String changeId
  ) {
    var spatialState = new SpatialState();

    getOperationsGoingIntoChange(
        licencePositionViewService.getCorrectedChronologicalPositions(licenceCorrection, licencePositionId),
        licencePositionId,
        changeId
    ).forEach(spatialState::apply);

    return spatialState;
  }

  private List<Feature> toBlockFeatures(Set<UUID> featureIds) {
    if (featureIds.isEmpty()) {
      return List.of();
    }

    return featureService.getFeaturesByIds(featureIds)
        .stream()
        .filter(LicenceBlockFeatureUtil::isLicenceBlock)
        .sorted(BLOCK_ORDER)
        .toList();
  }

  private static List<LicenceOperation> getOperationsGoingIntoChange(
      List<ChronologicalPosition> chronologicalPositions,
      UUID positionId,
      @Nullable String changeId
  ) {
    var operations = new ArrayList<LicenceOperation>();

    for (var chronologicalPosition : chronologicalPositions) {
      if (chronologicalPosition.id().equals(positionId)) {
        operations.addAll(getOperationsBefore(chronologicalPosition.changes(), changeId));
        return operations;
      }

      operations.addAll(getOperationsBefore(chronologicalPosition.changes(), null));
    }

    return operations;
  }

  private static List<LicenceOperation> getOperationsBefore(
      List<PositionChange> changes,
      @Nullable String stopBeforeChangeId
  ) {
    return changes.stream()
        .takeWhile(change -> !Objects.equals(change.changeId(), stopBeforeChangeId))
        .filter(change -> !Objects.equals(change.changeType(), LicencePositionChangeType.REMOVE_CHANGE))
        .flatMap(change -> change.operations().stream())
        .toList();
  }
}
