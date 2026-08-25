package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.positionDateAndOrderUnchanged;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;

@Service
public class PartialSurrenderCorrectionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final FeatureService featureService;
  private final CommandJourneyService commandJourneyService;

  public PartialSurrenderCorrectionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      FeatureService featureService,
      CommandJourneyService commandJourneyService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.featureService = featureService;
    this.commandJourneyService = commandJourneyService;
  }

  public Optional<PartialSurrenderOperation> getCommittedPartialSurrender(
      @Nullable LicencePositionCorrection licencePositionCorrection
  ) {
    if (licencePositionCorrection == null) {
      return Optional.empty();
    }

    return LicencePositionChangeOperationUtil.findOperations(
            licencePositionCorrection.getPayload().changes(), PartialSurrenderOperation.class)
        .stream()
        .findFirst();
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
        .orElseThrow(() -> new IllegalStateException(
            "Change with id %s is not a partial surrender change".formatted(liveChangeId)));
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
        payload.changes(), PartialSurrenderOperation.class, originalChangeId, operation);

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
    licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .ifPresent(this::removeStagedPartialSurrender);
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
    if (operation.featureIds().isEmpty()) {
      return false;
    }

    return operation.featureIds().stream()
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

    var surrenderableIds = getSurrenderableBlockFeatures(licencePositionCorrection).stream()
        .map(Feature::getId)
        .collect(Collectors.toSet());

    var retainedIds = committedPartialSurrender.get().featureIds().stream()
        .filter(surrenderableIds::contains)
        .toList();

    if (retainedIds.size() == committedPartialSurrender.get().featureIds().size()) {
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
        : List.of(LicenceOperation.newPartialSurrenderOperation()
            .withSurrenderDate(committedPartialSurrender.get().surrenderDate())
            .withFeatureIds(retainedIds)
            .withSurrenderDetails(retainedSurrenderDetails)
            .build());

    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        PartialSurrenderOperation.class,
        operations
    );
  }

  public List<Feature> getSurrenderableBlockFeatures(LicencePositionCorrection licencePositionCorrection) {
    return switch (licencePositionCorrection.getPayload()) {
      case CreateLicencePositionPayload create -> licencePositionService.getBlockFeaturesOnLicenceOnOrBefore(
          licencePositionCorrection.getLicenceCorrection().getLicence(),
          create.effectiveDate(),
          create.effectiveDateOrder());
      case UpdateLicencePositionPayload ignored -> licencePositionService.getBlockFeatures(
          licencePositionCorrection.getTargetLicencePosition());
    };
  }

  public List<Feature> getSurrenderableBlockFeatures(LicencePosition licencePosition) {
    return licencePositionService.getBlockFeatures(licencePosition);
  }

  public Feature getSurrenderedBlockFeatureOrThrow(
      LicencePositionCorrection licencePositionCorrection,
      UUID featureId
  ) {
    return getSurrenderedBlockFeatureOrThrow(
        getCommittedPartialSurrenderOrThrow(licencePositionCorrection), featureId);
  }

  public Feature getSurrenderedBlockFeatureOrThrow(PartialSurrenderOperation operation, UUID featureId) {
    if (!operation.featureIds().contains(featureId)) {
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
        .withFeatureIds(operation.featureIds())
        .withSurrenderDetails(featureIdToSurrenderDetails)
        .build();
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

    return licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection, PartialSurrenderOperation.class, List.of(operation));
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
}
