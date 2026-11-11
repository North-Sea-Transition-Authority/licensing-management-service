package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity.SubareaSetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@Service
public class SubareaChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionSpatialService licencePositionSpatialService;
  private final FeatureService featureService;
  private final CommandJourneyService commandJourneyService;
  private final SubareaSetEquityCorrectionService subareaSetEquityCorrectionService;

  public SubareaChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionSpatialService licencePositionSpatialService,
      FeatureService featureService,
      CommandJourneyService commandJourneyService,
      SubareaSetEquityCorrectionService subareaSetEquityCorrectionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.featureService = featureService;
    this.commandJourneyService = commandJourneyService;
    this.subareaSetEquityCorrectionService = subareaSetEquityCorrectionService;
  }

  @Transactional
  public LicencePositionCorrection commitSubareaChangeForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      UUID blockFeatureId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    return licencePositionCorrectionService.replaceAddChangeForExecutedPosition(
        licenceCorrection,
        licencePosition,
        positionCorrection,
        SubareaOperation.class,
        isForBlock(blockFeatureId),
        List.of(newSubareaOperation(positionCorrection, blockFeatureId))
    );
  }

  @Transactional
  public void commitSubareaChange(
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId
  ) {
    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        SubareaOperation.class,
        isForBlock(blockFeatureId),
        List.of(newSubareaOperation(licencePositionCorrection, blockFeatureId))
    );
  }

  @Transactional
  public LicencePositionCorrection correctExistingSubareaChange(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);
    var liveChangeId = liveChange.getId().toString();
    var liveOperation = getLiveSubareaOperationOrThrow(liveChange);

    var hasStagedJourney = licencePositionCorrectionService.findStagedChange(positionCorrection, liveChangeId)
        .flatMap(change -> licencePositionCorrectionService.findStagedChangeOperation(change, SubareaOperation.class))
        .map(SubareaOperation::commandJourneyId)
        .filter(commandJourneyId -> !commandJourneyId.equals(liveOperation.commandJourneyId()))
        .isPresent();

    if (!hasStagedJourney) {
      stageUpdateWithNewJourney(positionCorrection, liveChangeId, liveOperation.blockFeatureId());
    }
    return positionCorrection;
  }

  @Transactional
  public UUID getOrCreateCommandJourney(
      LicencePositionCorrection positionCorrection,
      UUID blockFeatureId
  ) {
    var change = getSubareaChangeOrThrow(positionCorrection, blockFeatureId);
    var operation = getSubareaOperationForChangeOrThrow(change);

    if (operation.commandJourneyId() == null && change instanceof UpdateChangeOperations) {
      return stageUpdateWithNewJourney(positionCorrection, change.changeId(), blockFeatureId);
    }
    return operation.commandJourneyIdOrThrow();
  }

  /**
   * Records the active features from a command journey onto the operation and checks to see if lineage is lost for each subarea
   * attribute (e.g. beneficial interests/operators/etc), if it is not lost then the relevant operation is staged for the new
   * subarea.
   */
  @Transactional
  public void completeSubareaChange(
      LicencePositionCorrection positionCorrection,
      UUID blockFeatureId
  ) {
    var change = getSubareaChangeOrThrow(positionCorrection, blockFeatureId);
    var operation = getSubareaOperationForChangeOrThrow(change);
    var stateGoingIntoChange = licencePositionSpatialService.getStateGoingIntoChange(
        positionCorrection,
        change.changeId()
    );
    var stateWithoutChange = licencePositionSpatialService.getStateAtEndOfPositionWithoutChange(
        positionCorrection,
        change.changeId()
    );
    var originalSubareas = stateGoingIntoChange.subareasOf(blockFeatureId);

    var initialFeatureIdsByOutputFeatureId = commandJourneyService
        .getInitialFeatureIdsByCreatedFeatureId(operation.commandJourneyIdOrThrow());
    var replacedFeatureIds = initialFeatureIdsByOutputFeatureId.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
    var outputSubareas = featureService.getFeaturesByIds(initialFeatureIdsByOutputFeatureId.keySet()).stream()
        .map(SubareaDetails::from)
        .toList();

    var replacedSubareas = originalSubareas.stream()
        .filter(subarea -> replacedFeatureIds.contains(subarea.featureId()))
        .toList();

    var completedOperation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(blockFeatureId)
        .withReplacedSubareas(replacedSubareas)
        .withOutputSubareas(outputSubareas)
        .withCommandJourneyId(operation.commandJourneyId())
        .build();
    var payload = positionCorrection.getPayload();
    var changes = payload.changes().stream()
        .map(stagedChange -> stagedChange.changeId().equals(change.changeId())
            ? rebuildKeepingChangeId(stagedChange, completedOperation)
            : stagedChange)
        .toList();
    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);

    for (var outputSubarea : outputSubareas) {
      var lineageSubareas = originalSubareas.stream()
          .filter(subarea -> initialFeatureIdsByOutputFeatureId.get(outputSubarea.featureId()).contains(subarea.featureId()))
          .toList();
      subareaSetEquityCorrectionService.cascadeSubareaEquity(
          positionCorrection,
          stateWithoutChange,
          blockFeatureId,
          lineageSubareas,
          outputSubarea
      );
    }
  }

  private static LicencePositionChangeType rebuildKeepingChangeId(
      LicencePositionChangeType change,
      LicenceOperation operation
  ) {
    var rebuiltChange = LicencePositionChangeOperationUtil.rebuildWith(change, operation);
    return rebuiltChange instanceof AddChange addChange
        ? new AddChange(change.changeId(), addChange.changeOrder(), addChange.operations())
        : rebuiltChange;
  }

  private LicencePositionChangeType getSubareaChangeOrThrow(
      LicencePositionCorrection positionCorrection,
      UUID blockFeatureId
  ) {
    return positionCorrection.getPayload()
        .changes()
        .stream()
        .filter(stagedChange -> LicencePositionChangeOperationUtil
            .findOperations(List.of(stagedChange), SubareaOperation.class).stream()
            .anyMatch(operation -> blockFeatureId.equals(operation.blockFeatureId())))
        .findFirst()
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No subarea change staged for block %s on position correction %s"
                .formatted(blockFeatureId, positionCorrection.getId())));
  }

  private UUID stageUpdateWithNewJourney(
      LicencePositionCorrection positionCorrection,
      String changeId,
      UUID blockFeatureId
  ) {
    var commandJourneyId = createCommandJourneyForSubareas(positionCorrection, blockFeatureId, changeId);
    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(blockFeatureId)
        .withCommandJourneyId(commandJourneyId)
        .build();

    var payload = positionCorrection.getPayload();
    var changes = LicencePositionChangeOperationUtil.upsertUpdateChangeById(payload.changes(), changeId, operation);

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
    return commandJourneyId;
  }

  public SubareaOperation getSubareaOperationOrThrow(
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId
  ) {
    return LicencePositionChangeOperationUtil
        .findOperations(licencePositionCorrection.getPayload().changes(), SubareaOperation.class)
        .stream()
        .filter(operation -> blockFeatureId.equals(operation.blockFeatureId()))
        .findFirst()
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No subarea change staged for block %s on position correction %s"
                .formatted(blockFeatureId, licencePositionCorrection.getId())));
  }

  public SubareaOperation getStagedSubareaOperationOrThrow(
      LicencePositionCorrection positionCorrection,
      String changeId
  ) {
    return getSubareaOperationForChangeOrThrow(
        licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId));
  }

  public SubareaOperation getLiveSubareaOperationOrThrow(LicencePositionChange liveChange) {
    return LicencePositionChangeOperationUtil.findOperation(liveChange, SubareaOperation.class)
        .orElseThrow(() -> new IllegalStateException(
            "Change with id %s is not a subarea change".formatted(liveChange.getId())
        ));
  }

  public String getBlockName(SubareaOperation subareaOperation) {
    return featureService.getFeatureOrThrow(subareaOperation.blockFeatureId()).getFeatureName();
  }

  @Transactional
  public void undoSubareaChange(
      LicencePositionCorrection positionCorrection,
      String changeId
  ) {
    var change = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId);
    var subareaOperation = getSubareaOperationForChangeOrThrow(change);

    switch (change) {
      case AddChange ignored -> deleteCommandJourney(subareaOperation);
      case UpdateChangeOperations ignored -> deleteCommandJourney(subareaOperation);
      default -> {
        // a staged removal or reorder creates no command journey
      }
    }

    licencePositionCorrectionService.dropStagedChange(positionCorrection, changeId);
  }

  private void deleteCommandJourney(SubareaOperation subareaOperation) {
    if (subareaOperation.commandJourneyId() != null) {
      commandJourneyService.deleteCommandJourney(subareaOperation.commandJourneyId());
    }
  }

  private SubareaOperation getSubareaOperationForChangeOrThrow(LicencePositionChangeType change) {
    return licencePositionCorrectionService.findStagedChangeOperation(change, SubareaOperation.class)
        .orElseThrow(() -> new IllegalStateException(
            "Change with id %s is not a subarea change".formatted(change.changeId())));
  }

  private SubareaOperation newSubareaOperation(
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId
  ) {
    return LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(blockFeatureId)
        .withCommandJourneyId(createCommandJourneyForSubareas(licencePositionCorrection, blockFeatureId, null))
        .build();
  }

  /**
   * Creates a command journey seeded with the block's subareas as they stand going into the given change.
   *
   * @param licencePositionCorrection the position correction the subarea change is staged on
   * @param blockFeatureId            the block whose subareas seed the journey
   * @param changeId                  the change being corrected, or null when adding a new change, in which case the subareas are
   *                                  taken after every change on the position
   * @return the id of the created command journey
   */
  private UUID createCommandJourneyForSubareas(
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId,
      @Nullable String changeId
  ) {
    var subareas = licencePositionSpatialService.getSubareasGoingIntoChange(
        licencePositionCorrection,
        blockFeatureId,
        changeId
    );
    var subareaFeatures = featureService.getFeaturesByIds(SubareaDetails.featureIds(subareas));
    return commandJourneyService.createAndAssignCommandJourney(subareaFeatures).getId();
  }

  private static Predicate<SubareaOperation> isForBlock(UUID blockFeatureId) {
    return operation -> blockFeatureId.equals(operation.blockFeatureId());
  }
}
