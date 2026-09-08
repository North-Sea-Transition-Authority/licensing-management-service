package uk.co.nstauthority.licensingmanagementservice.licence.position.spatial;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil.BLOCK_ORDER;

import jakarta.annotation.Nullable;
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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
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
        getFeatureIdsBeforeChange(
            licencePositionViewService.getCorrectedChronologicalPositions(licenceCorrection, licencePositionId),
            licencePositionId,
            changeId
        )
    );
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

  private static Set<UUID> getFeatureIdsBeforeChange(
      List<ChronologicalPosition> chronologicalPositions,
      UUID positionId,
      @Nullable String changeId
  ) {
    var featureIds = getFeatureIdsBefore(chronologicalPositions, positionId);

    return chronologicalPositions.stream()
        .filter(chronologicalPosition -> chronologicalPosition.id().equals(positionId))
        .findFirst()
        .map(chronologicalPosition -> getFeatureIdsAfterChanges(featureIds, chronologicalPosition.changes(), changeId))
        .orElse(featureIds);
  }

  private static Set<UUID> getFeatureIdsBefore(List<ChronologicalPosition> chronologicalPositions, UUID positionId) {
    var featureIds = Set.<UUID>of();

    for (var chronologicalPosition : chronologicalPositions) {
      if (chronologicalPosition.id().equals(positionId)) {
        return featureIds;
      }

      featureIds = getFeatureIdsAfterChanges(featureIds, chronologicalPosition.changes(), null);
    }

    return featureIds;
  }

  private static Set<UUID> getFeatureIdsAfterChanges(
      Set<UUID> featureIds,
      List<PositionChange> changes,
      @Nullable String stopBeforeChangeId
  ) {
    var currentFeatureIds = featureIds;

    for (var operation : getOperationsBefore(changes, stopBeforeChangeId)) {
      currentFeatureIds = getFeatureIdsAfterOperation(currentFeatureIds, operation);
    }

    return currentFeatureIds;
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

  private static Set<UUID> getFeatureIdsAfterOperation(
      Set<UUID> featureIdsBeforeOperation,
      LicenceOperation currentOperation
  ) {
    return switch (currentOperation) {
      case PartialSurrenderOperation partialSurrenderOperation ->
          partialSurrenderOperation.outputFeatureIds().isEmpty()
              ? featureIdsBeforeOperation
              : Set.copyOf(partialSurrenderOperation.outputFeatureIds());
      case AdministratorOperation ignored -> featureIdsBeforeOperation;
      case SetEquityOperation ignored -> featureIdsBeforeOperation;
      case TransferEquityOperation ignored -> featureIdsBeforeOperation;
      case SubareaOperation ignored -> featureIdsBeforeOperation;
    };
  }
}
