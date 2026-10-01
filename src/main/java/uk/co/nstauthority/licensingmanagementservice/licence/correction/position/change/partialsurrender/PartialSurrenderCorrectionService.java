package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.positionDateAndOrderUnchanged;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil.BLOCK_ORDER;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@Service
public class PartialSurrenderCorrectionService {

  private static final String NOT_A_PARTIAL_SURRENDER = "Change with id %s is not a partial surrender change";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionSpatialService licencePositionSpatialService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final FeatureService featureService;
  private final CommandJourneyService commandJourneyService;
  private final PartialSurrenderSubareaService partialSurrenderSubareaService;

  public PartialSurrenderCorrectionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionSpatialService licencePositionSpatialService,
      LicencePositionChangeService licencePositionChangeService,
      FeatureService featureService,
      CommandJourneyService commandJourneyService,
      PartialSurrenderSubareaService partialSurrenderSubareaService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.featureService = featureService;
    this.commandJourneyService = commandJourneyService;
    this.partialSurrenderSubareaService = partialSurrenderSubareaService;
  }

  public Optional<PartialSurrenderOperation> getCommittedPartialSurrender(
      @Nullable LicencePositionCorrection licencePositionCorrection
  ) {
    return licencePositionCorrectionService.getCommittedChangeOfType(licencePositionCorrection, PartialSurrenderOperation.class);
  }

  public Optional<String> getCommittedPartialSurrenderChangeId(
      @Nullable LicencePositionCorrection licencePositionCorrection
  ) {
    if (licencePositionCorrection == null) {
      return Optional.empty();
    }
    return LicencePositionChangeOperationUtil
        .findChange(licencePositionCorrection.getPayload().changes(), PartialSurrenderOperation.class)
        .map(LicencePositionChangeType::changeId);
  }

  public PartialSurrenderOperation getCommittedPartialSurrenderOrThrow(
      LicencePositionCorrection licencePositionCorrection
  ) {
    return getCommittedPartialSurrender(licencePositionCorrection)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No partial surrender staged on licence position correction %s"
                .formatted(licencePositionCorrection.getId())));
  }

  public PartialSurrenderOperation getLiveSurrenderOrThrow(String liveChangeId) {
    var liveChange = licencePositionChangeService.getByIdOrThrow(UUID.fromString(liveChangeId));

    return LicencePositionChangeOperationUtil.findOperation(liveChange, PartialSurrenderOperation.class)
        .orElseThrow(() -> new IllegalStateException(NOT_A_PARTIAL_SURRENDER.formatted(liveChangeId)));
  }

  public Optional<String> findCorrectedLiveChangeId(LicencePositionCorrection licencePositionCorrection) {
    return LicencePositionChangeOperationUtil
        .findChange(licencePositionCorrection.getPayload().changes(), PartialSurrenderOperation.class)
        .filter(UpdateChangeOperations.class::isInstance)
        .map(LicencePositionChangeType::changeId);
  }

  public PartialSurrenderOperation getSurrenderUnderCorrectionOrThrow(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      String liveChangeId
  ) {
    return getCommittedPartialSurrender(
        licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
            .orElse(null))
        .orElseGet(() -> getLiveSurrenderOrThrow(liveChangeId));
  }

  @Transactional
  public LicencePositionCorrection commitPartialSurrender(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation operation
  ) {
    return applyPartialSurrender(licencePositionCorrection, operation);
  }

  @Transactional
  public LicencePositionCorrection commitPartialSurrenderForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      PartialSurrenderOperation operation
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    return applyPartialSurrender(positionCorrection, operation);
  }

  @Transactional
  public LicencePositionCorrection correctExistingPartialSurrender(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      String originalChangeId,
      PartialSurrenderOperation operation
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    deleteOrphanedCommandJourneys(positionCorrection, operation);

    var staged = getCommittedPartialSurrender(positionCorrection);
    var processedOperation = withProcessedSubareas(
        licenceCorrection,
        licencePosition.getId(),
        originalChangeId,
        operation,
        () -> staged.or(() -> Optional.of(getLiveSurrenderOrThrow(originalChangeId))));
    staged.ifPresent(stagedSurrender ->
        deleteCorrectionOwnedCroppedSubareas(stagedSurrender, processedOperation, originalChangeId));

    var payload = positionCorrection.getPayload();

    var changes = LicencePositionChangeOperationUtil.upsertUpdateChange(
        payload.changes(),
        PartialSurrenderOperation.class,
        originalChangeId,
        processedOperation
    );

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    return licencePositionCorrectionService.save(positionCorrection);
  }

  public PartialSurrenderOperation getOrCreatePartialSurrenderDetails(
      PartialSurrenderOperation operation,
      UUID featureId,
      BlockSurrenderType blockSurrenderType
  ) {
    var existing = operation.featureIdToSurrenderDetails().get(featureId);
    var surrenderDetails = reuseOrCreateJourneyWithType(operation, featureId, existing, blockSurrenderType);
    return withSurrenderDetails(operation, featureId, surrenderDetails);
  }

  private SurrenderDetails reuseOrCreateJourneyWithType(
      PartialSurrenderOperation operation,
      UUID featureId,
      @Nullable SurrenderDetails existing,
      BlockSurrenderType blockSurrenderType
  ) {
    var reusable = existing != null && existing.hasCommandJourney() ? existing : null;

    var commandJourneyId = reusable != null
        ? reusable.commandJourneyId()
        : commandJourneyService
            .createAndAssignCommandJourney(List.of(getSurrenderedBlockFeatureOrThrow(operation, featureId)))
            .getId();

    return new SurrenderDetails(
        blockSurrenderType,
        commandJourneyId,
        surrenderedFeatureIdsFor(reusable, featureId, blockSurrenderType)
    );
  }

  @Transactional
  public void revertPartialSurrenderCorrection(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      PartialSurrenderOperation discardedSurrender
  ) {
    var positionCorrection =
        licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition);

    // the discarded surrender may carry a journey just created for it, which is staged nowhere
    var discardedCommandJourneyIds = new LinkedHashSet<>(commandJourneyIdsOf(discardedSurrender));

    positionCorrection.ifPresent(pc -> {
      getCommittedPartialSurrender(pc).ifPresent(staged -> {
        discardedCommandJourneyIds.addAll(commandJourneyIdsOf(staged));
        deleteCorrectionOwnedCroppedSubareas(staged, null, findCorrectedLiveChangeId(pc).orElse(null));
      });
      removeStagedPartialSurrender(pc);
    });

    discardedCommandJourneyIds.forEach(commandJourneyService::deleteCommandJourney);
  }

  @Transactional
  public void removeExistingPartialSurrender(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String changeId
  ) {
    licencePositionCorrectionService.stageRemovalOfExecutedChange(licenceCorrection, licencePosition, changeId);
  }

  @Transactional
  public void undoPartialSurrenderChange(LicenceCorrection licenceCorrection, String changeId) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);
    var changeToUndo = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId);

    var surrender = getPartialSurrenderForChangeOrThrow(changeToUndo);

    switch (changeToUndo) {
      case AddChange ignored -> deleteCorrectionOwnedJourneysAndSubareas(surrender, null);
      case UpdateChangeOperations updateChange -> deleteCorrectionOwnedJourneysAndSubareas(surrender, updateChange.changeId());
      case RemoveChange ignored -> {
        // a staged removal creates no command journeys or cropped subareas
      }
      case UpdateChangeOrder ignored -> {
        // a staged reorder creates no command journeys or cropped subareas
      }
    }

    licencePositionCorrectionService.dropStagedChange(positionCorrection, changeId);
  }

  /**
   * The blocks a surrender gives up, whether invalid or not.
   */
  public List<Feature> getSurrenderedBlockFeatures(PartialSurrenderOperation surrender) {
    return featureService.getFeaturesByIds(surrender.surrenderedFeatureIds())
        .stream()
        .sorted(BLOCK_ORDER)
        .toList();
  }

  public List<PartialSurrenderChangeView.BlockRow> getBlockRows(PartialSurrenderOperation surrender) {
    var blockNamesById = getSurrenderedBlockFeatures(surrender)
        .stream()
        .collect(Collectors.toMap(Feature::getId, Feature::getFeatureName));

    return surrender.surrenderedFeatureIds().stream()
        .map(featureId -> new PartialSurrenderChangeView.BlockRow(
            blockNamesById.getOrDefault(featureId, NOT_AVAILABLE),
            surrender.surrenderTypeDisplayName(featureId)))
        .toList();
  }

  public PartialSurrenderOperation getStagedPartialSurrenderOrThrow(
      LicencePositionCorrection positionCorrection,
      String changeId
  ) {
    return getPartialSurrenderForChangeOrThrow(
        licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId));
  }

  public boolean hasStagedPartialSurrender(LicencePositionCorrection licencePositionCorrection) {
    return getCommittedPartialSurrender(licencePositionCorrection).isPresent();
  }

  public boolean allSurrenderedBlocksAreFull(LicencePositionCorrection licencePositionCorrection) {
    return getCommittedPartialSurrender(licencePositionCorrection)
        .map(this::allSurrenderedBlocksAreFull)
        .orElse(false);
  }

  public boolean allSurrenderedBlocksAreFull(PartialSurrenderOperation operation) {
    if (operation.surrenderedFeatureIds().isEmpty()) {
      return false;
    }

    return operation.surrenderedFeatureIds().stream()
        .allMatch(id -> {
          var surrenderDetails = operation.featureIdToSurrenderDetails().get(id);
          return surrenderDetails != null && surrenderDetails.type() == BlockSurrenderType.FULL_SURRENDER;
        });
  }

  @Transactional
  public void adjustPartialSurrenderBlocks(LicencePositionCorrection licencePositionCorrection) {
    var committedPartialSurrender = getCommittedPartialSurrender(licencePositionCorrection);
    if (committedPartialSurrender.isEmpty()) {
      return;
    }

    var surrenderableIds = licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(
            licencePositionCorrection,
            getCommittedPartialSurrenderChangeId(licencePositionCorrection).orElse(null))
        .stream()
        .map(Feature::getId)
        .collect(Collectors.toSet());

    var retainedIds = committedPartialSurrender.get().surrenderedFeatureIds().stream()
        .filter(surrenderableIds::contains)
        .toList();

    if (retainedIds.size() == committedPartialSurrender.get().surrenderedFeatureIds().size()) {
      return;
    }

    deleteCommandJourneysForRemovedBlocks(
        committedPartialSurrender.get().featureIdToSurrenderDetails(),
        surrenderableIds::contains
    );
    var retainedSurrenderDetails = committedPartialSurrender.get().featureIdToSurrenderDetails().entrySet().stream()
        .filter(entry -> surrenderableIds.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    var retainedOperation = retainedIds.isEmpty()
        ? null
        : LicenceOperation.newPartialSurrenderOperation()
            .withSurrenderDate(committedPartialSurrender.get().surrenderDate())
            .withSurrenderedFeatureIds(retainedIds)
            .withSurrenderDetails(retainedSurrenderDetails)
            .build();

    deleteCorrectionOwnedCroppedSubareas(
        committedPartialSurrender.get(),
        retainedOperation,
        findCorrectedLiveChangeId(licencePositionCorrection).orElse(null));

    if (retainedOperation == null) {
      unstageSurrender(licencePositionCorrection);
      return;
    }

    restageSurrender(licencePositionCorrection, retainedOperation);
  }

  public Feature getSurrenderedBlockFeatureOrThrow(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId
  ) {
    return getSurrenderedBlockFeatureOrThrow(
        getCommittedPartialSurrenderOrThrow(licencePositionCorrection), featureId);
  }

  public Feature getSurrenderedBlockFeatureOrThrow(PartialSurrenderOperation operation, UUID featureId) {
    if (!operation.surrenderedFeatureIds().contains(featureId)) {
      throw new LmsEntityNotFoundException(
          "Block %s is not surrendered by partial surrender %s".formatted(featureId, operation.id())
      );
    }

    return featureService.getFeatureOrThrow(featureId);
  }

  public SurrenderDetails getSurrenderDetailsOrThrow(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId
  ) {
    var surrenderDetails = getCommittedPartialSurrenderOrThrow(licencePositionCorrection)
        .featureIdToSurrenderDetails()
        .get(featureId);

    if (surrenderDetails == null) {
      throw new LmsEntityNotFoundException(
          "No surrender detail staged for block %s on position correction %s"
              .formatted(featureId, licencePositionCorrection.getId()));
    }

    return surrenderDetails;
  }

  @Transactional
  public void setBlockSurrenderType(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId,
      BlockSurrenderType blockSurrenderType
  ) {
    var operation = getCommittedPartialSurrenderOrThrow(licencePositionCorrection);
    var existing = operation.featureIdToSurrenderDetails().get(featureId);

    var surrenderDetails = resolveSurrenderDetails(licencePositionCorrection, featureId, existing, blockSurrenderType);

    applyPartialSurrender(licencePositionCorrection, withSurrenderDetails(operation, featureId, surrenderDetails));
  }

  @Transactional
  public void setSurrenderedFeatureIds(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId,
      Collection<UUID> surrenderedFeatureIds
  ) {
    var partialSurrenderOperation = getCommittedPartialSurrenderOrThrow(licencePositionCorrection);
    var existing = getSurrenderDetailsOrThrow(licencePositionCorrection, featureId);

    var updated = new SurrenderDetails(
        existing.type(),
        existing.commandJourneyId(),
        surrenderedFeatureIds.stream().toList()
    );

    applyPartialSurrender(licencePositionCorrection, withSurrenderDetails(partialSurrenderOperation, featureId, updated));
  }

  @Transactional
  public void clearSurrenderedIds(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId,
      Collection<UUID> activeFeatureIds
  ) {
    var partialSurrenderOperation = getCommittedPartialSurrenderOrThrow(licencePositionCorrection);
    var existing = partialSurrenderOperation.featureIdToSurrenderDetails().get(featureId);
    if (existing == null
        || existing.surrenderedFeatureIds().isEmpty()
        || activeFeatureIds.containsAll(existing.surrenderedFeatureIds())
    ) {
      return;
    }
    var cleared = new SurrenderDetails(existing.type(), existing.commandJourneyId(), List.of());
    applyPartialSurrender(licencePositionCorrection, withSurrenderDetails(partialSurrenderOperation, featureId, cleared));
  }

  private SurrenderDetails resolveSurrenderDetails(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId,
      @Nullable SurrenderDetails existing,
      BlockSurrenderType blockSurrenderType
  ) {
    // A migrated block re-confirmed as the type it already has still needs a journey making, so having one is part
    // of what counts as unchanged.
    var unchangedType = existing != null && existing.type() == blockSurrenderType && existing.hasCommandJourney();

    var commandJourneyId = unchangedType
        ? existing.commandJourneyId()
        : createCommandJourneyFor(licencePositionCorrection, featureId);

    return new SurrenderDetails(
        blockSurrenderType,
        commandJourneyId,
        surrenderedFeatureIdsFor(existing, featureId, blockSurrenderType)
    );
  }

  private static List<UUID> surrenderedFeatureIdsFor(
      @Nullable SurrenderDetails existing,
      UUID featureId,
      BlockSurrenderType blockSurrenderType
  ) {
    if (blockSurrenderType == BlockSurrenderType.FULL_SURRENDER) {
      return List.of(featureId);
    }
    return existing != null && existing.type() == blockSurrenderType ? existing.surrenderedFeatureIds() : List.of();
  }

  private UUID createCommandJourneyFor(LicencePositionCorrection licencePositionCorrection, UUID featureId) {
    var feature = getSurrenderedBlockFeatureOrThrow(licencePositionCorrection, featureId);
    return commandJourneyService.createAndAssignCommandJourney(List.of(feature)).getId();
  }

  private PartialSurrenderOperation withSurrenderDetails(
      PartialSurrenderOperation operation,
      UUID featureId,
      SurrenderDetails surrenderDetails
  ) {
    var featureIdToSurrenderDetails = new HashMap<>(operation.featureIdToSurrenderDetails());
    featureIdToSurrenderDetails.put(featureId, surrenderDetails);

    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(operation.surrenderDate())
        .withSurrenderedFeatureIds(operation.surrenderedFeatureIds())
        .withSurrenderDetails(featureIdToSurrenderDetails)
        .build();
  }

  private PartialSurrenderOperation getPartialSurrenderForChangeOrThrow(LicencePositionChangeType change) {
    return licencePositionCorrectionService.resolveStagedChangeOperations(change).stream()
        .filter(PartialSurrenderOperation.class::isInstance)
        .map(PartialSurrenderOperation.class::cast)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(NOT_A_PARTIAL_SURRENDER.formatted(change.changeId())));
  }

  private void removeStagedPartialSurrender(LicencePositionCorrection licencePositionCorrection) {
    var payload = licencePositionCorrection.getPayload();
    var remainingChanges = LicencePositionChangeOperationUtil.removeChangesOf(
        payload.changes(), PartialSurrenderOperation.class);

    if (remainingChanges.isEmpty() && positionDateAndOrderUnchanged(licencePositionCorrection)) {
      licencePositionCorrectionService.delete(licencePositionCorrection);
      return;
    }

    licencePositionCorrection.setPayload(LicencePositionPayload.withChanges(payload, remainingChanges));
    licencePositionCorrectionService.save(licencePositionCorrection);
  }

  private LicencePositionCorrection applyPartialSurrender(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation operation
  ) {
    deleteOrphanedCommandJourneys(licencePositionCorrection, operation);

    var staged = getCommittedPartialSurrender(licencePositionCorrection);
    var processedOperation = withProcessedSubareas(
        licencePositionCorrection.getLicenceCorrection(),
        licencePositionCorrection.getPositionId(),
        getCommittedPartialSurrenderChangeId(licencePositionCorrection).orElse(null),
        operation,
        () -> staged
    );
    staged.ifPresent(stagedSurrender -> deleteCorrectionOwnedCroppedSubareas(
        stagedSurrender,
        processedOperation,
        findCorrectedLiveChangeId(licencePositionCorrection).orElse(null))
    );

    return licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        PartialSurrenderOperation.class,
        List.of(processedOperation)
    );
  }

  private void restageSurrender(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation retainedSurrender
  ) {
    var payload = licencePositionCorrection.getPayload();
    var stagedSurrender = LicencePositionChangeOperationUtil.findChange(
        payload.changes(),
        PartialSurrenderOperation.class
    );

    if (stagedSurrender.orElse(null) instanceof UpdateChangeOperations) {
      licencePositionCorrection.setPayload(
          LicencePositionPayload.withChanges(
              payload,
              LicencePositionChangeOperationUtil.replaceOperation(
                  payload.changes(),
                  PartialSurrenderOperation.class,
                  retainedSurrender
              )
          )
      );
      licencePositionCorrectionService.save(licencePositionCorrection);
      return;
    }

    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        PartialSurrenderOperation.class,
        List.of(retainedSurrender)
    );
  }

  private void unstageSurrender(LicencePositionCorrection licencePositionCorrection) {
    var stagedSurrender = LicencePositionChangeOperationUtil.findChange(
        licencePositionCorrection.getPayload().changes(),
        PartialSurrenderOperation.class
    );

    if (stagedSurrender.orElse(null) instanceof UpdateChangeOperations updateChange) {
      var liveChange = licencePositionChangeService.getByIdOrThrow(UUID.fromString(updateChange.changeId()));

      licencePositionCorrectionService.dropStagedChange(licencePositionCorrection, updateChange.changeId());
      licencePositionCorrectionService.stageChangesOnPosition(
          licencePositionCorrection.getLicenceCorrection(),
          liveChange.getLicencePosition().getId(),
          List.of(LicencePositionChangeType.removeChange().withChangeId(updateChange.changeId()).build())
      );
      return;
    }

    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        PartialSurrenderOperation.class,
        List.of()
    );
  }

  /**
   * Works out what surrendering each block does to its subareas.
   */
  private PartialSurrenderOperation withProcessedSubareas(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      @Nullable String changeId,
      PartialSurrenderOperation operation,
      Supplier<Optional<PartialSurrenderOperation>> previousSurrender
  ) {
    if (!licenceCorrection.getLicence().getType().isProduction()) {
      return operation;
    }

    var previous = previousSurrender.get();
    var previousStates = previous.map(PartialSurrenderOperation::surrenderStateByFeatureId).orElse(Map.of());
    var states = operation.surrenderStateByFeatureId();

    var blockIdToSurrenderDetailsWithSubareas = operation
        .featureIdToSurrenderDetails()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
          var blockFeatureId = entry.getKey();
          var surrenderDetails = entry.getValue();

          var previousDetails = Objects.equals(previousStates.get(blockFeatureId), states.get(blockFeatureId))
              ? previous.orElseThrow().featureIdToSurrenderDetails().get(blockFeatureId)
              : null;

          return partialSurrenderSubareaService.processSubareas(
              licenceCorrection,
              licencePositionId,
              changeId,
              blockFeatureId,
              surrenderDetails,
              getRetainedFeatureIds(surrenderDetails),
              previousDetails);
        }));

    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(operation.surrenderDate())
        .withSurrenderedFeatureIds(operation.surrenderedFeatureIds())
        .withSurrenderDetails(blockIdToSurrenderDetailsWithSubareas)
        .build();
  }

  private void deleteCorrectionOwnedCroppedSubareas(
      PartialSurrenderOperation stagedSurrender,
      @Nullable PartialSurrenderOperation replacementSurrender,
      @Nullable String liveChangeId
  ) {
    var orphanedFeatureIds = new HashSet<>(croppedSubareaFeatureIdsOf(stagedSurrender));
    if (orphanedFeatureIds.isEmpty()) {
      return;
    }

    if (replacementSurrender != null) {
      orphanedFeatureIds.removeAll(croppedSubareaFeatureIdsOf(replacementSurrender));
    }
    if (liveChangeId != null) {
      orphanedFeatureIds.removeAll(croppedSubareaFeatureIdsOf(getLiveSurrenderOrThrow(liveChangeId)));
    }

    deleteCroppedSubareas(orphanedFeatureIds);
  }

  private void deleteCroppedSubareas(Set<UUID> croppedSubareaFeatureIds) {
    if (!croppedSubareaFeatureIds.isEmpty()) {

      featureService.deleteAll(featureService.getFeaturesByIds(croppedSubareaFeatureIds));
    }
  }

  private void deleteOrphanedCommandJourneys(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation newOperation
  ) {
    getCommittedPartialSurrender(licencePositionCorrection).ifPresent(staged -> {
      var retainedCommandJourneyIds = commandJourneyIdsOf(newOperation);
      commandJourneyIdsOf(staged).stream()
          .filter(commandJourneyId -> !retainedCommandJourneyIds.contains(commandJourneyId))
          .forEach(commandJourneyService::deleteCommandJourney);
    });
  }

  private void deleteCommandJourneysForRemovedBlocks(
      Map<UUID, SurrenderDetails> featureIdToSurrenderDetails,
      Predicate<UUID> retainFeatureId
  ) {
    featureIdToSurrenderDetails.entrySet().stream()
        .filter(entry -> !retainFeatureId.test(entry.getKey()))
        .filter(entry -> entry.getValue().hasCommandJourney())
        .map(entry -> entry.getValue().commandJourneyId())
        .forEach(commandJourneyService::deleteCommandJourney);
  }

  private void deleteCorrectionOwnedJourneysAndSubareas(
      PartialSurrenderOperation stagedSurrender,
      @Nullable String liveChangeId
  ) {
    commandJourneyIdsOf(stagedSurrender).forEach(commandJourneyService::deleteCommandJourney);
    deleteCorrectionOwnedCroppedSubareas(stagedSurrender, null, liveChangeId);
  }

  public List<UUID> getRetainedFeatureIds(SurrenderDetails surrenderDetails) {
    if (!surrenderDetails.hasCommandJourney()) {
      return surrenderDetails.retainedFeatureIds();
    }

    if (surrenderDetails.surrenderedFeatureIds().isEmpty()) {
      return List.of();
    }

    var surrenderedFeatureIds = Set.copyOf(surrenderDetails.surrenderedFeatureIds());
    return commandJourneyService.getActiveFeatures(surrenderDetails.commandJourneyId()).stream()
        .filter(feature -> !surrenderedFeatureIds.contains(feature.getId()))
        .sorted(Comparator.comparing(Feature::getFeatureName))
        .map(Feature::getId)
        .toList();
  }

  public PartialSurrenderOperation toExecutedSurrender(PartialSurrenderOperation stagedSurrender) {
    var executedSurrenderDetails = stagedSurrender.featureIdToSurrenderDetails().entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              var surrenderDetails = entry.getValue();
              return new SurrenderDetails(
                  surrenderDetails.type(),
                  null,
                  surrenderDetails.surrenderedFeatureIds(),
                  getRetainedFeatureIds(surrenderDetails),
                  surrenderDetails.retainedFeatureIdToSubareas()
              );
            })
        );

    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(stagedSurrender.surrenderDate())
        .withSurrenderedFeatureIds(stagedSurrender.surrenderedFeatureIds())
        .withSurrenderDetails(executedSurrenderDetails)
        .build();
  }

  public static Set<UUID> commandJourneyIdsOf(PartialSurrenderOperation surrender) {
    return surrender.featureIdToSurrenderDetails().values().stream()
        .filter(SurrenderDetails::hasCommandJourney)
        .map(SurrenderDetails::commandJourneyId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static Set<UUID> croppedSubareaFeatureIdsOf(PartialSurrenderOperation surrender) {
    return surrender.featureIdToSurrenderDetails().values().stream()
        .flatMap(details -> details.croppedSubareaFeatureIds().stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
