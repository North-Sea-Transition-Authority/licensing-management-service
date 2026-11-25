package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity.SubareaSetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@Service
public class SubareaChangeService {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final LicencePositionSpatialService licencePositionSpatialService;
  private final FeatureService featureService;
  private final CommandJourneyService commandJourneyService;
  private final SubareaSetEquityCorrectionService subareaSetEquityCorrectionService;

  SubareaChangeService(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      LicencePositionSpatialService licencePositionSpatialService,
      FeatureService featureService,
      CommandJourneyService commandJourneyService,
      SubareaSetEquityCorrectionService subareaSetEquityCorrectionService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.featureService = featureService;
    this.commandJourneyService = commandJourneyService;
    this.subareaSetEquityCorrectionService = subareaSetEquityCorrectionService;
  }

  /**
   * Stages a subarea change for the block, returning the position staged payload it is staged on.
   */
  @Transactional
  public LicencePositionStagedPayload commitSubareaChange(CorrectionPosition correctionPosition, UUID blockFeatureId) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed(LicenceCorrection correction, LicencePosition licencePosition) ->
          commitSubareaChangeForExecutedPosition(correction, licencePosition, blockFeatureId);
      case CorrectionPosition.Staged(LicencePositionStagedPayload licencePositionStagedPayload) -> {
        commitSubareaChange(licencePositionStagedPayload, blockFeatureId);
        yield licencePositionStagedPayload;
      }
    };
  }

  private LicencePositionStagedPayload commitSubareaChangeForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      UUID blockFeatureId
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition);

    return licencePositionStagedPayloadService.replaceAddChangeForExecutedPosition(
        licenceCorrection,
        licencePosition,
        positionStagedPayload,
        SubareaOperation.class,
        isForBlock(blockFeatureId),
        List.of(newSubareaOperation(positionStagedPayload, blockFeatureId))
    );
  }

  @Transactional
  public void commitSubareaChange(
      LicencePositionStagedPayload licencePositionStagedPayload,
      UUID blockFeatureId
  ) {
    licencePositionStagedPayloadService.replaceAddChangeFor(
        licencePositionStagedPayload,
        SubareaOperation.class,
        isForBlock(blockFeatureId),
        List.of(newSubareaOperation(licencePositionStagedPayload, blockFeatureId))
    );
  }

  @Transactional
  public LicencePositionStagedPayload correctExistingSubareaChange(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition);
    var liveChangeId = liveChange.getId().toString();
    var liveOperation = getLiveSubareaOperationOrThrow(liveChange);

    var hasStagedJourney = licencePositionStagedPayloadService.findStagedChange(positionStagedPayload, liveChangeId)
        .flatMap(change -> licencePositionStagedPayloadService.findStagedChangeOperation(change, SubareaOperation.class))
        .map(SubareaOperation::commandJourneyId)
        .filter(commandJourneyId -> !commandJourneyId.equals(liveOperation.commandJourneyId()))
        .isPresent();

    if (!hasStagedJourney) {
      stageUpdateWithNewJourney(positionStagedPayload, liveChangeId, liveOperation.blockFeatureId());
    }
    return positionStagedPayload;
  }

  @Transactional
  public UUID getOrCreateCommandJourney(
      LicencePositionStagedPayload positionStagedPayload,
      UUID blockFeatureId
  ) {
    var change = getSubareaChangeOrThrow(positionStagedPayload, blockFeatureId);
    var operation = getSubareaOperationForChangeOrThrow(change);

    if (operation.commandJourneyId() == null && change instanceof UpdateChangeOperations) {
      return stageUpdateWithNewJourney(positionStagedPayload, change.changeId(), blockFeatureId);
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
      LicencePositionStagedPayload positionStagedPayload,
      UUID blockFeatureId
  ) {
    var change = getSubareaChangeOrThrow(positionStagedPayload, blockFeatureId);
    var operation = getSubareaOperationForChangeOrThrow(change);
    var stateGoingIntoChange = licencePositionSpatialService.getStateGoingIntoChange(
        positionStagedPayload,
        change.changeId()
    );
    var stateWithoutChange = licencePositionSpatialService.getStateAtEndOfPositionWithoutChange(
        positionStagedPayload,
        change.changeId()
    );
    var transfers = getTransfers(operation.commandJourneyIdOrThrow(), stateGoingIntoChange.subareasOf(blockFeatureId));

    var completedOperation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(blockFeatureId)
        .withTransfers(transfers)
        .withCommandJourneyId(operation.commandJourneyId())
        .build();
    var payload = positionStagedPayload.getPayload();
    var changes = payload.changes().stream()
        .map(stagedChange -> stagedChange.changeId().equals(change.changeId())
            ? rebuildKeepingChangeId(stagedChange, completedOperation)
            : stagedChange)
        .toList();
    positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionStagedPayloadService.save(positionStagedPayload);

    subareaSetEquityCorrectionService.cascadeSubareaEquity(
        positionStagedPayload,
        stateWithoutChange,
        blockFeatureId,
        transfers
    );
  }

  public List<SubareaTransfer> getTransfers(
      UUID commandJourneyId,
      List<SubareaDetails> originalSubareas
  ) {
    var outputFeatureIdToInitialFeatureIds = commandJourneyService.getInitialFeatureIdsByCreatedFeatureId(commandJourneyId);
    var initialFeatureIdsToOutputSubareas = featureService.getFeaturesByIds(outputFeatureIdToInitialFeatureIds.keySet())
        .stream()
        .map(SubareaDetails::from)
        .collect(Collectors.groupingBy(
            outputSubarea -> outputFeatureIdToInitialFeatureIds.get(outputSubarea.featureId()),
            LinkedHashMap::new,
            Collectors.toList()
        ));

    return initialFeatureIdsToOutputSubareas.entrySet().stream()
        .map(entry -> new SubareaTransfer(subareasWithFeatureIds(originalSubareas, entry.getKey()), entry.getValue()))
        .toList();
  }

  private static List<SubareaDetails> subareasWithFeatureIds(
      List<SubareaDetails> subareas,
      Set<UUID> featureIds
  ) {
    return subareas.stream()
        .filter(subarea -> featureIds.contains(subarea.featureId()))
        .toList();
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

  public LicencePositionChangeType getSubareaChangeOrThrow(
      LicencePositionStagedPayload positionStagedPayload,
      UUID blockFeatureId
  ) {
    return positionStagedPayload.getPayload()
        .changes()
        .stream()
        .filter(stagedChange -> LicencePositionChangeOperationUtil
            .findOperations(List.of(stagedChange), SubareaOperation.class).stream()
            .anyMatch(operation -> blockFeatureId.equals(operation.blockFeatureId())))
        .findFirst()
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No subarea change staged for block %s on position staged payload %s"
                .formatted(blockFeatureId, positionStagedPayload.getId())));
  }

  private UUID stageUpdateWithNewJourney(
      LicencePositionStagedPayload positionStagedPayload,
      String changeId,
      UUID blockFeatureId
  ) {
    var commandJourneyId = createCommandJourneyForSubareas(positionStagedPayload, blockFeatureId, changeId);
    var operation = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(blockFeatureId)
        .withCommandJourneyId(commandJourneyId)
        .build();

    var payload = positionStagedPayload.getPayload();
    var changes = LicencePositionChangeOperationUtil.upsertUpdateChangeById(payload.changes(), changeId, operation);

    positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionStagedPayloadService.save(positionStagedPayload);
    return commandJourneyId;
  }

  public SubareaOperation getSubareaOperationOrThrow(
      LicencePositionStagedPayload licencePositionStagedPayload,
      UUID blockFeatureId
  ) {
    return LicencePositionChangeOperationUtil
        .findOperations(licencePositionStagedPayload.getPayload().changes(), SubareaOperation.class)
        .stream()
        .filter(operation -> blockFeatureId.equals(operation.blockFeatureId()))
        .findFirst()
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No subarea change staged for block %s on position staged payload %s"
                .formatted(blockFeatureId, licencePositionStagedPayload.getId())));
  }

  public SubareaOperation getStagedSubareaOperationOrThrow(
      LicencePositionStagedPayload positionStagedPayload,
      String changeId
  ) {
    return getSubareaOperationForChangeOrThrow(
        licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, changeId));
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

  /**
   * Deletes the command journey a staged subarea change created, ahead of the change being dropped.
   */
  @Transactional
  public void deleteStagedSubareaArtefacts(LicencePositionChangeType stagedChange) {
    var subareaOperation = getSubareaOperationForChangeOrThrow(stagedChange);

    switch (stagedChange) {
      case AddChange ignored -> deleteCommandJourney(subareaOperation);
      case UpdateChangeOperations ignored -> deleteCommandJourney(subareaOperation);
      default -> {
        // a staged removal or reorder creates no command journey
      }
    }
  }

  private void deleteCommandJourney(SubareaOperation subareaOperation) {
    if (subareaOperation.commandJourneyId() != null) {
      commandJourneyService.deleteCommandJourney(subareaOperation.commandJourneyId());
    }
  }

  private SubareaOperation getSubareaOperationForChangeOrThrow(LicencePositionChangeType change) {
    return licencePositionStagedPayloadService.findStagedChangeOperation(change, SubareaOperation.class)
        .orElseThrow(() -> new IllegalStateException(
            "Change with id %s is not a subarea change".formatted(change.changeId())));
  }

  private SubareaOperation newSubareaOperation(
      LicencePositionStagedPayload licencePositionStagedPayload,
      UUID blockFeatureId
  ) {
    return LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(blockFeatureId)
        .withCommandJourneyId(createCommandJourneyForSubareas(licencePositionStagedPayload, blockFeatureId, null))
        .build();
  }

  /**
   * Creates a command journey seeded with the block's subareas as they stand going into the given change.
   *
   * @param licencePositionStagedPayload the position staged payload the subarea change is staged on
   * @param blockFeatureId            the block whose subareas seed the journey
   * @param changeId                  the change being corrected, or null when adding a new change, in which case the subareas are
   *                                  taken after every change on the position
   * @return the id of the created command journey
   */
  private UUID createCommandJourneyForSubareas(
      LicencePositionStagedPayload licencePositionStagedPayload,
      UUID blockFeatureId,
      @Nullable String changeId
  ) {
    var subareas = licencePositionSpatialService.getSubareasGoingIntoChange(
        licencePositionStagedPayload,
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
