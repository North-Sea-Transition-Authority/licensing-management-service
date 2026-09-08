package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.positionDateAndOrderUnchanged;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
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
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@Service
public class PartialSurrenderCorrectionService {

  private static final String NOT_A_PARTIAL_SURRENDER = "Change with id %s is not a partial surrender change";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionSpatialService licencePositionSpatialService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicencePositionViewService licencePositionViewService;
  private final FeatureService featureService;
  private final CommandJourneyService commandJourneyService;

  public PartialSurrenderCorrectionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionSpatialService licencePositionSpatialService,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionViewService licencePositionViewService,
      FeatureService featureService,
      CommandJourneyService commandJourneyService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licencePositionViewService = licencePositionViewService;
    this.featureService = featureService;
    this.commandJourneyService = commandJourneyService;
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
    var payload = positionCorrection.getPayload();

    var changes = LicencePositionChangeOperationUtil.upsertUpdateChange(
        payload.changes(),
        PartialSurrenderOperation.class,
        originalChangeId,
        withRecalculatedOutputs(positionCorrection, operation, originalChangeId));

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    var saved = licencePositionCorrectionService.save(positionCorrection);

    recalculateOutputsAfter(
        licenceCorrection, licencePosition.getId(), getCommittedPartialSurrenderChangeId(saved).orElse(null));

    return saved;
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
    var commandJourneyId = existing != null
        ? existing.commandJourneyId()
        : commandJourneyService
            .createAndAssignCommandJourney(List.of(getSurrenderedBlockFeatureOrThrow(operation, featureId)))
            .getId();

    return new SurrenderDetails(
        blockSurrenderType,
        commandJourneyId,
        surrenderedFeatureIdsFor(existing, featureId, blockSurrenderType)
    );
  }

  @Transactional
  public void revertPartialSurrenderCorrection(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    var positionCorrection =
        licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition);
    var correctedLiveChangeId = positionCorrection.flatMap(this::findCorrectedLiveChangeId);

    positionCorrection.ifPresent(this::removeStagedPartialSurrender);

    recalculateOutputsAfter(licenceCorrection, licencePosition.getId(), correctedLiveChangeId.orElse(null));
  }

  @Transactional
  public void removeExistingPartialSurrender(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String changeId
  ) {
    licencePositionCorrectionService.stageRemovalOfExecutedChange(licenceCorrection, licencePosition, changeId);

    recalculateOutputsAfter(licenceCorrection, licencePosition.getId(), changeId);
  }

  /**
   * Drops a surrender change staged by this correction, whether it was added, corrected or removed.
   *
   * <p>Only the command journeys owned exclusively by this correction are deleted: any journey the live surrender
   * also holds is retained. A staged removal never created any journeys of its own, so nothing is deleted for it.</p>
   */
  @Transactional
  public void undoPartialSurrenderChange(LicenceCorrection licenceCorrection, String changeId) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);
    var changeToUndo = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId);

    var surrender = getPartialSurrenderForChangeOrThrow(changeToUndo);

    correctionOwnedCommandJourneyIds(changeToUndo, surrender).forEach(commandJourneyService::deleteCommandJourney);

    licencePositionCorrectionService.dropStagedChange(positionCorrection, changeId);
  }

  public List<PartialSurrenderChangeView.BlockRow> getBlockRows(PartialSurrenderOperation surrender) {
    var blockNamesById = featureService.getFeaturesByIds(surrender.surrenderedFeatureIds())
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

    var operations = retainedIds.isEmpty()
        ? List.<PartialSurrenderOperation>of()
        : List.of(withRecalculatedOutputs(licencePositionCorrection, LicenceOperation.newPartialSurrenderOperation()
            .withSurrenderDate(committedPartialSurrender.get().surrenderDate())
            .withSurrenderedFeatureIds(retainedIds)
            .withSurrenderDetails(retainedSurrenderDetails)
            .build(), null));

    var saved = licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        PartialSurrenderOperation.class,
        operations
    );

    recalculateOutputsAfter(
        licencePositionCorrection.getLicenceCorrection(),
        licencePositionCorrection.getPositionId(),
        getCommittedPartialSurrenderChangeId(saved).orElse(null));
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
    var unchangedType = existing != null && existing.type() == blockSurrenderType;

    var commandJourneyId = unchangedType
        ? existing.commandJourneyId()
        : recreateCommandJourney(licencePositionCorrection, featureId, existing);

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

  private UUID recreateCommandJourney(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId,
      @Nullable SurrenderDetails existing
  ) {
    if (existing != null) {
      commandJourneyService.deleteCommandJourney(existing.commandJourneyId());
    }
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

    var saved = licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        PartialSurrenderOperation.class,
        List.of(withRecalculatedOutputs(licencePositionCorrection, operation, null)));

    recalculateOutputsAfter(
        licencePositionCorrection.getLicenceCorrection(),
        licencePositionCorrection.getPositionId(),
        getCommittedPartialSurrenderChangeId(saved).orElse(null));

    return saved;
  }

  /**
   * A surrender's outputs are the input features of whatever spatial change comes next, so writing one leaves every
   * later surrender's outputs stale. Positions are walked in chronological order and each is saved before the next is
   * resolved, so a recomputed set feeds into the surrender that follows it.
   *
   * <p>On the changed position itself only the changes ordered after {@code changeId} are restaged - the caller has
   * already recalculated the change it wrote, and anything before that change is unaffected by it.
   */
  private void recalculateOutputsAfter(
      LicenceCorrection licenceCorrection,
      UUID positionId,
      @Nullable String changeId
  ) {
    var chronologicalPositions =
        licencePositionViewService.getCorrectedChronologicalPositions(licenceCorrection, positionId);

    var reachedChangedPosition = false;

    for (var position : chronologicalPositions) {
      if (!reachedChangedPosition) {
        reachedChangedPosition = position.id().equals(positionId);

        if (reachedChangedPosition) {
          recalculateOutputsFor(licenceCorrection, position, changesAfter(position, changeId));
        }

        continue;
      }

      recalculateOutputsFor(licenceCorrection, position, position.changes());
    }
  }

  private static List<PositionChange> changesAfter(ChronologicalPosition position, @Nullable String changeId) {
    if (changeId == null) {
      return List.of();
    }

    var changesAfter = new ArrayList<PositionChange>();
    var reachedChange = false;

    for (var change : position.changes()) {
      if (reachedChange) {
        changesAfter.add(change);
        continue;
      }

      reachedChange = Objects.equals(change.changeId(), changeId);
    }

    return changesAfter;
  }

  private void recalculateOutputsFor(
      LicenceCorrection licenceCorrection,
      ChronologicalPosition position,
      List<PositionChange> changes
  ) {
    if (changes.isEmpty()) {
      return;
    }

    var liveChangesById = licencePositionChangeService.findByLicencePositionId(position.id())
        .stream()
        .collect(Collectors.toMap(liveChange -> liveChange.getId().toString(), Function.identity()));

    changes.stream()
        .filter(change -> !LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType()))
        .forEach(change -> change.operations().stream()
            .filter(PartialSurrenderOperation.class::isInstance)
            .map(PartialSurrenderOperation.class::cast)
            .forEach(surrender -> restageOutputs(
                licenceCorrection, position.id(), change.changeId(), surrender, liveChangesById)));
  }

  private void restageOutputs(
      LicenceCorrection licenceCorrection,
      UUID positionId,
      String changeId,
      PartialSurrenderOperation surrender,
      Map<String, LicencePositionChange> liveChangesById
  ) {
    var recalculated = withRecalculatedOutputs(licenceCorrection, positionId, changeId, surrender);

    if (new HashSet<>(recalculated.outputFeatureIds()).equals(new HashSet<>(surrender.outputFeatureIds()))) {
      return;
    }

    var liveChange = liveChangesById.get(changeId);
    // An executed change is corrected by staging an update change against it, whereas one this correction added is
    // already held on a position correction and so is updated in place.
    var positionCorrection = liveChange != null
        ? licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(
            licenceCorrection, liveChange.getLicencePosition())
        : licencePositionCorrectionService.getPositionCorrectionContainingChange(licenceCorrection, changeId);

    var payload = positionCorrection.getPayload();
    var changes = LicencePositionChangeOperationUtil.upsertUpdateChange(
        payload.changes(),
        PartialSurrenderOperation.class,
        changeId,
        recalculated);

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }

  /**
   * Recalculates the blocks the licence is left holding once this surrender has been applied. These are what the next
   * spatial change works from, so they are recomputed on every write rather than being maintained incrementally.
   */
  private PartialSurrenderOperation withRecalculatedOutputs(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation operation,
      @Nullable String executedChangeId
  ) {
    // TODO EPGF-192: Change when criteria for complete partial surrender exists - see SurrenderDetails#isComplete
    if (!allSurrenderedBlocksAreFull(operation)) {
      return withOutputFeatureIds(operation, List.of());
    }

    var changeId = getCommittedPartialSurrenderChangeId(licencePositionCorrection)
        .orElse(executedChangeId);

    return withOutputsFrom(
        operation, licencePositionSpatialService.getBlockFeaturesGoingIntoChange(licencePositionCorrection, changeId));
  }

  private PartialSurrenderOperation withRecalculatedOutputs(
      LicenceCorrection licenceCorrection,
      UUID positionId,
      String changeId,
      PartialSurrenderOperation operation
  ) {
    // TODO EPGF-192: Change when criteria for complete partial surrender exists - see SurrenderDetails#isComplete
    if (!allSurrenderedBlocksAreFull(operation)) {
      return withOutputFeatureIds(operation, List.of());
    }

    return withOutputsFrom(
        operation,
        licencePositionSpatialService.getBlockFeaturesGoingIntoChange(licenceCorrection, positionId, changeId));
  }

  /**
   * Only blocks are recorded. The subareas a licence holds follow from the blocks it holds and the subareas' own start
   * and end dates, so a surrendered block's subareas need no recording here to stay reachable: they remain linked to
   * the block, which the position before this surrender still held.
   */
  private static PartialSurrenderOperation withOutputsFrom(
      PartialSurrenderOperation operation,
      List<Feature> blocksGoingIntoTheSurrender
  ) {
    var surrenderedFeatureIds = new HashSet<>(operation.surrenderedFeatureIds());

    return withOutputFeatureIds(operation, blocksGoingIntoTheSurrender.stream()
        .map(Feature::getId)
        .filter(featureId -> !surrenderedFeatureIds.contains(featureId))
        .toList());
  }

  private static PartialSurrenderOperation withOutputFeatureIds(
      PartialSurrenderOperation operation,
      List<UUID> outputFeatureIds
  ) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(operation.surrenderDate())
        .withSurrenderedFeatureIds(operation.surrenderedFeatureIds())
        .withSurrenderDetails(operation.featureIdToSurrenderDetails())
        .withOutputFeatureIds(outputFeatureIds)
        .build();
  }

  private void deleteOrphanedCommandJourneys(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation newOperation
  ) {
    getCommittedPartialSurrender(licencePositionCorrection)
        .ifPresent(existing -> deleteCommandJourneysForRemovedBlocks(
            existing.featureIdToSurrenderDetails(),
            newOperation.featureIdToSurrenderDetails()::containsKey
        ));
  }

  private void deleteCommandJourneysForRemovedBlocks(
      Map<UUID, SurrenderDetails> featureIdToSurrenderDetails,
      Predicate<UUID> retainFeatureId
  ) {
    featureIdToSurrenderDetails.entrySet().stream()
        .filter(entry -> !retainFeatureId.test(entry.getKey()))
        .forEach(entry -> commandJourneyService.deleteCommandJourney(entry.getValue().commandJourneyId()));
  }

  private Set<UUID> correctionOwnedCommandJourneyIds(
      LicencePositionChangeType changeToUndo,
      PartialSurrenderOperation stagedSurrender
  ) {
    return switch (changeToUndo) {
      case AddChange ignored -> commandJourneyIdsOf(stagedSurrender);
      case UpdateChangeOperations correctedChange -> {
        var ownedCommandJourneyIds = new LinkedHashSet<>(commandJourneyIdsOf(stagedSurrender));
        ownedCommandJourneyIds.removeAll(commandJourneyIdsOf(getLiveSurrenderOrThrow(correctedChange.changeId())));
        yield ownedCommandJourneyIds;
      }
      case RemoveChange ignored -> Set.of();
      case UpdateChangeOrder ignored -> Set.of();
    };
  }

  private static Set<UUID> commandJourneyIdsOf(PartialSurrenderOperation surrender) {
    return surrender.featureIdToSurrenderDetails().values().stream()
        .map(SurrenderDetails::commandJourneyId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
