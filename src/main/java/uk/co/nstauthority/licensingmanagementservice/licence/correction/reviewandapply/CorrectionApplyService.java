package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.ownedgisartefact.CorrectionOwnedGisArtefactService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicencePositionValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;
import uk.co.nstauthority.licensingmanagementservice.util.ListUtil;

@Service
public class CorrectionApplyService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CorrectionApplyService.class);

  static final LicencePositionChangeStatus APPLIED_CHANGE_STATUS = LicencePositionChangeStatus.CONSENTED;

  private final CorrectedTimelineService correctedTimelineService;
  private final LicencePositionValidationService licencePositionValidationService;
  private final LicenceCorrectionService licenceCorrectionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicenceTransactionService licenceTransactionService;
  private final LicencePositionRepository licencePositionRepository;
  private final LicencePositionChangeRepository licencePositionChangeRepository;
  private final LicenceTransactionRepository licenceTransactionRepository;
  private final EntityManager entityManager;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final CommandJourneyService commandJourneyService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceStatusService licenceStatusService;
  private final FeatureService featureService;
  private final CorrectionOwnedGisArtefactService correctionOwnedGisArtefactService;

  public CorrectionApplyService(
      CorrectedTimelineService correctedTimelineService,
      LicencePositionValidationService licencePositionValidationService,
      LicenceCorrectionService licenceCorrectionService,
      LicencePositionChangeService licencePositionChangeService,
      LicenceTransactionService licenceTransactionService,
      LicencePositionRepository licencePositionRepository,
      LicencePositionChangeRepository licencePositionChangeRepository,
      LicenceTransactionRepository licenceTransactionRepository,
      EntityManager entityManager,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      CommandJourneyService commandJourneyService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceStatusService licenceStatusService,
      FeatureService featureService,
      CorrectionOwnedGisArtefactService correctionOwnedGisArtefactService
  ) {
    this.correctedTimelineService = correctedTimelineService;
    this.licencePositionValidationService = licencePositionValidationService;
    this.licenceCorrectionService = licenceCorrectionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licenceTransactionService = licenceTransactionService;
    this.licencePositionRepository = licencePositionRepository;
    this.licencePositionChangeRepository = licencePositionChangeRepository;
    this.licenceTransactionRepository = licenceTransactionRepository;
    this.entityManager = entityManager;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.commandJourneyService = commandJourneyService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceStatusService = licenceStatusService;
    this.featureService = featureService;
    this.correctionOwnedGisArtefactService = correctionOwnedGisArtefactService;
  }

  @Transactional
  public List<PositionValidationError> applyCorrection(LicenceCorrection correction) {
    var licenceCorrection =
        entityManager.find(LicenceCorrection.class, correction.getId(), LockModeType.PESSIMISTIC_WRITE);

    var licenceStatusType = licenceStatusService.getCurrentStatus(licenceCorrection.getLicence());

    if (licenceCorrection.getStatus() != LicenceCorrectionStatus.IN_PROGRESS) {
      throw new IllegalStateException(
          "Cannot apply licence correction %s as it is %s"
              .formatted(licenceCorrection.getId(), licenceCorrection.getStatus()));
    }

    var correctedTimeline = correctedTimelineService.getCorrectedTimeline(licenceCorrection);
    var blockingErrors = licencePositionValidationService.validate(
        correctedTimeline.positionsToApply(),
        correctedTimeline.resolvedStates(),
        correctedTimeline.positionStagedPayloads(),
        correctedTimeline.licenceType(),
        licenceStatusType
    );

    if (!blockingErrors.isEmpty()) {
      return blockingErrors;
    }

    var positionStagedPayloads = correctedTimeline.positionStagedPayloads();
    var appliedCommandJourneyIds = new LinkedHashSet<UUID>();

    var stagedCommandJourneyIds = stagedCommandJourneyIdsOf(positionStagedPayloads);
    correctionOwnedGisArtefactService.deleteJourneysNotIn(licenceCorrection, stagedCommandJourneyIds);

    var replacedLiveSubareaJourneyIds = commandJourneyService.findExistingCommandJourneyIds(
        replacedLiveSubareaJourneyIdsOf(positionStagedPayloads, stagedCommandJourneyIds)
    );

    revertReplacedPartialSurrenders(positionStagedPayloads);

    if (!correctedTimeline.displayedPositions().isEmpty()) {
      var latestState = getLatestState(correctedTimeline.resolvedStates(), correctedTimeline.displayedPositions());

      licenceResponsibleOrganisationService.saveLicenseesFromForm(
          correction.getLicence(),
          ListUtil.toStrings(latestState.licenseeIds().stream().toList())
      );
    }

    positionStagedPayloadsOfType(positionStagedPayloads, LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .forEach(this::updatePosition);

    var addedPositionsById = new HashMap<UUID, LicencePosition>();
    positionStagedPayloadsOfType(positionStagedPayloads, LicencePositionStagedPayloadChangeType.ADD_POSITION)
        .forEach(positionStagedPayload -> addedPositionsById.put(
            positionStagedPayload.getPositionId(),
            addPosition(licenceCorrection, positionStagedPayload)));

    var mergedRetainedSourceFeatureIds = mergedRetainedSourceFeatureIdsOf(positionStagedPayloads);

    positionStagedPayloads.stream()
        .filter(positionStagedPayload ->
            positionStagedPayload.getChangeType() != LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .forEach(positionStagedPayload ->
            applyStagedChanges(positionStagedPayload, addedPositionsById, appliedCommandJourneyIds));

    positionStagedPayloadsOfType(positionStagedPayloads, LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .forEach(this::removePosition);

    var replacedLiveSubareaFeatureIds = replacedLiveSubareaJourneyIds.stream()
        .flatMap(commandJourneyId -> commandJourneyService
            .getInitialFeatureIdsByCreatedFeatureId(commandJourneyId).keySet().stream())
        .collect(Collectors.toSet());

    appliedCommandJourneyIds.forEach(commandJourneyService::deleteAllExcludingActiveFeatures);
    replacedLiveSubareaJourneyIds.forEach(commandJourneyService::deleteAllExcludingActiveFeatures);

    if (!mergedRetainedSourceFeatureIds.isEmpty()) {
      commandJourneyService.deleteFeaturesNotHeldByAJourney(
          featureService.getFeaturesByIds(mergedRetainedSourceFeatureIds));
    }

    correctionOwnedGisArtefactService.deleteUnreferencedFeatures(licenceCorrection, replacedLiveSubareaFeatureIds);

    licenceCorrectionService.completeCorrection(licenceCorrection);

    LOGGER.info(
        "Applied licence correction {} ({}) to licence {} with {} position staged payloads",
        licenceCorrection.getId(),
        licenceCorrection.getCorrectionReference(),
        licenceCorrection.getLicence().getId(),
        positionStagedPayloads.size()
    );

    return List.of();
  }

  private LicencePositionState getLatestState(ResolvedStates states, List<ChronologicalPosition> chronologicalPositions) {
    var latestPosition = chronologicalPositions.stream()
        .max(Comparator.comparing(ChronologicalPosition::date)
          .thenComparing(ChronologicalPosition::order))
        .orElseThrow(() -> new IllegalStateException("No chronological positions to resolve latest state"));
    return states.currentState(latestPosition.id());
  }

  private void updatePosition(LicencePositionStagedPayload positionStagedPayload) {
    var payload = (UpdateLicencePositionPayload) positionStagedPayload.getPayload();
    var licencePosition = positionStagedPayload.getTargetLicencePosition();

    if (payload.effectiveDate() != null) {
      licencePosition.setPositionDate(payload.effectiveDate());
    }

    if (payload.effectiveDateOrder() != null) {
      licencePosition.setPositionDateOrder(payload.effectiveDateOrder());
    }

    if (payload.correctionReference() != null
        && !payload.correctionReference().equals(licencePosition.getLicenceTransaction().getRegulatorReference())) {
      licencePosition.setLicenceTransaction(
          licenceTransactionService.createLicenceTransaction(payload.correctionReference()));
    }

    licencePositionRepository.save(licencePosition);
  }

  private LicencePosition addPosition(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayload positionStagedPayload
  ) {
    var payload = createPayloadOf(positionStagedPayload);
    var licence = licenceCorrection.getLicence();
    var licenceTransactionId = UUID.fromString(payload.licenceTransactionId());
    var correctionReference = Objects.requireNonNullElse(
        payload.correctionReference(), licenceCorrection.getCorrectionReference());

    var licenceTransaction = licenceTransactionRepository.findById(licenceTransactionId)
        .orElseGet(() -> licenceTransactionService.createLicenceTransaction(
            licenceTransactionId, correctionReference));

    var licencePosition = new LicencePosition(positionStagedPayload.getPositionId());
    licencePosition.setLicence(licence);
    licencePosition.setLicenceTransaction(licenceTransaction);
    licencePosition.setPositionDate(payload.effectiveDate());
    licencePosition.setPositionDateOrder(payload.effectiveDateOrder());
    licencePosition.setStatus(LicencePositionStatus.EXECUTED);

    entityManager.persist(licencePosition);

    return licencePosition;
  }

  private void removePosition(LicencePositionStagedPayload positionStagedPayload) {
    var licencePosition = positionStagedPayload.getTargetLicencePosition();
    licencePosition.setStatus(LicencePositionStatus.REMOVED);

    licencePositionRepository.save(licencePosition);
  }

  private void applyStagedChanges(
      LicencePositionStagedPayload positionStagedPayload,
      Map<UUID, LicencePosition> addedPositionsById,
      Set<UUID> appliedCommandJourneyIds
  ) {
    var payload = positionStagedPayload.getPayload();
    var licencePosition = resolvePosition(positionStagedPayload, addedPositionsById);

    payload.changesOfType(RemoveChange.class).forEach(this::removeChange);
    payload.changesOfType(AddChange.class).forEach(change -> addChange(licencePosition, change, appliedCommandJourneyIds));
    payload.changesOfType(UpdateChangeOperations.class)
        .forEach(change -> updateChangeOperations(licencePosition, payload, change, appliedCommandJourneyIds));
    payload.changesOfType(UpdateChangeOrder.class).forEach(this::updateChangeOrder);

    if (payload instanceof UpdateLicencePositionPayload updatePayload && updatePayload.effectiveDate() != null) {
      setPartialSurrenderFeatureDates(licencePosition);
    }
  }

  private void removeChange(RemoveChange change) {
    licencePositionChangeService.findById(UUID.fromString(change.changeId()))
        .ifPresent(licencePositionChangeRepository::delete);
  }

  private void addChange(LicencePosition licencePosition, AddChange change, Set<UUID> appliedCommandJourneyIds) {
    licencePositionChangeService.createLicencePositionChange(
        UUID.fromString(change.changeId()),
        licencePosition,
        toExecutedOperations(licencePosition, LicencePositionChangeType.operationsOf(change), appliedCommandJourneyIds),
        change.changeOrder(),
        APPLIED_CHANGE_STATUS
    );
  }

  private void updateChangeOperations(
      LicencePosition licencePosition,
      LicencePositionPayload payload,
      UpdateChangeOperations change,
      Set<UUID> appliedCommandJourneyIds
  ) {
    var changeId = UUID.fromString(change.changeId());
    var operations = toExecutedOperations(
        licencePosition,
        LicencePositionChangeType.operationsOf(change),
        appliedCommandJourneyIds
    );

    licencePositionChangeService.findById(changeId).ifPresentOrElse(
        licencePositionChange -> {
          licencePositionChange.setLicencePosition(licencePosition);
          licencePositionChange.setOperations(operations);
          licencePositionChangeRepository.save(licencePositionChange);
        },
        () -> licencePositionChangeService.createLicencePositionChange(
            changeId,
            licencePosition,
            operations,
            stagedChangeOrder(payload, change.changeId()).orElseGet(() -> nextChangeOrder(licencePosition)),
            APPLIED_CHANGE_STATUS
        )
    );
  }

  private List<LicenceOperation> toExecutedOperations(
      LicencePosition licencePosition,
      List<LicenceOperation> stagedOperations,
      Set<UUID> appliedCommandJourneyIds
  ) {
    return stagedOperations.stream()
        .map(operation -> {
          if (operation instanceof PartialSurrenderOperation stagedSurrender) {
            appliedCommandJourneyIds.addAll(PartialSurrenderCorrectionService.commandJourneyIdsOf(stagedSurrender));
            var executedSurrender = partialSurrenderCorrectionService.toExecutedSurrender(stagedSurrender);
            partialSurrenderCorrectionService.setFeatureDates(executedSurrender, licencePosition.getPositionDate());
            return executedSurrender;
          }
          return operation;
        })
        .toList();
  }

  private static Set<UUID> mergedRetainedSourceFeatureIdsOf(List<LicencePositionStagedPayload> positionStagedPayloads) {
    return positionStagedPayloads.stream()
        .filter(positionStagedPayload ->
            positionStagedPayload.getChangeType() != LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .filter(positionStagedPayload -> positionStagedPayload.getPayload() != null)
        .flatMap(positionStagedPayload -> positionStagedPayload.getPayload().changes().stream())
        .flatMap(change -> LicencePositionChangeType.operationsOf(change).stream())
        .filter(PartialSurrenderOperation.class::isInstance)
        .map(PartialSurrenderOperation.class::cast)
        .flatMap(partialSurrenderOperation -> partialSurrenderOperation.mergedRetainedSourceFeatureIds().stream())
        .collect(Collectors.toSet());
  }

  private static Set<UUID> stagedCommandJourneyIdsOf(List<LicencePositionStagedPayload> positionStagedPayloads) {
    return positionStagedPayloads.stream()
        .filter(positionStagedPayload ->
            positionStagedPayload.getChangeType() != LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .filter(positionStagedPayload -> positionStagedPayload.getPayload() != null)
        .flatMap(positionStagedPayload -> positionStagedPayload.getPayload().changes().stream())
        .flatMap(change -> LicencePositionChangeType.operationsOf(change).stream())
        .flatMap(operation -> switch (operation) {
          case PartialSurrenderOperation surrender ->
              PartialSurrenderCorrectionService.commandJourneyIdsOf(surrender).stream();
          case SubareaOperation subareaOperation -> Stream.ofNullable(subareaOperation.commandJourneyId());
          default -> Stream.empty();
        })
        .collect(Collectors.toSet());
  }

  private Set<UUID> replacedLiveSubareaJourneyIdsOf(
      List<LicencePositionStagedPayload> positionStagedPayloads,
      Set<UUID> stagedCommandJourneyIds
  ) {
    return positionStagedPayloads.stream()
        .flatMap(positionStagedPayload -> liveChangesReplacedBy(positionStagedPayload).stream())
        .flatMap(change -> LicencePositionChangeOperationUtil.findOperation(change, SubareaOperation.class).stream())
        .map(SubareaOperation::commandJourneyId)
        .filter(Objects::nonNull)
        .filter(commandJourneyId -> !stagedCommandJourneyIds.contains(commandJourneyId))
        .collect(Collectors.toSet());
  }

  private void revertReplacedPartialSurrenders(List<LicencePositionStagedPayload> positionStagedPayloads) {
    var stillCreatedFeatureIds = positionStagedPayloads.stream()
        .filter(positionStagedPayload -> positionStagedPayload.getPayload() != null)
        .flatMap(positionStagedPayload -> positionStagedPayload.getPayload().changes().stream())
        .flatMap(change -> LicencePositionChangeType.operationsOf(change).stream())
        .filter(PartialSurrenderOperation.class::isInstance)
        .map(PartialSurrenderOperation.class::cast)
        .flatMap(surrender -> surrender.createdFeatureIds().stream())
        .collect(Collectors.toSet());

    positionStagedPayloads.stream()
        .flatMap(positionStagedPayload -> liveChangesReplacedBy(positionStagedPayload).stream())
        .flatMap(change -> LicencePositionChangeOperationUtil
            .findOperation(change, PartialSurrenderOperation.class)
            .stream())
        .forEach(surrender -> partialSurrenderCorrectionService.revertFeatures(surrender, stillCreatedFeatureIds));
  }

  private List<LicencePositionChange> liveChangesReplacedBy(LicencePositionStagedPayload positionStagedPayload) {
    if (positionStagedPayload.getChangeType() == LicencePositionStagedPayloadChangeType.REMOVE_POSITION) {
      return licencePositionChangeService.findByLicencePositionId(positionStagedPayload.getTargetLicencePosition().getId());
    }

    return positionStagedPayload.getPayload().changes().stream()
        .filter(change -> change instanceof RemoveChange || change instanceof UpdateChangeOperations)
        .flatMap(change -> licencePositionChangeService.findById(UUID.fromString(change.changeId())).stream())
        .toList();
  }

  private void setPartialSurrenderFeatureDates(LicencePosition licencePosition) {
    licencePositionChangeService.findByLicencePositionId(licencePosition.getId()).stream()
        .flatMap(change -> LicencePositionChangeOperationUtil
            .findOperation(change, PartialSurrenderOperation.class)
            .stream())
        .forEach(surrender ->
            partialSurrenderCorrectionService.setFeatureDates(surrender, licencePosition.getPositionDate()));
  }

  private void updateChangeOrder(UpdateChangeOrder change) {
    licencePositionChangeService.findById(UUID.fromString(change.changeId()))
        .ifPresent(licencePositionChange -> {
          licencePositionChange.setChangeOrder(change.changeOrder());
          licencePositionChangeRepository.save(licencePositionChange);
        });
  }

  private static LicencePosition resolvePosition(
      LicencePositionStagedPayload positionStagedPayload,
      Map<UUID, LicencePosition> addedPositionsById
  ) {
    return switch (positionStagedPayload.getPayload()) {
      case CreateLicencePositionPayload ignored -> addedPositionsById.get(positionStagedPayload.getPositionId());
      case UpdateLicencePositionPayload ignored -> positionStagedPayload.getTargetLicencePosition();
    };
  }

  private int nextChangeOrder(LicencePosition licencePosition) {
    return licencePositionChangeService.findByLicencePositionId(licencePosition.getId())
        .stream()
        .mapToInt(LicencePositionChange::getChangeOrder)
        .max()
        .orElse(0) + 1;
  }

  private static Optional<Integer> stagedChangeOrder(LicencePositionPayload payload, String changeId) {
    return payload.changes()
        .stream()
        .filter(change -> changeId.equals(change.changeId()))
        .map(LicencePositionChangeType::ownedChangeOrder)
        .filter(Objects::nonNull)
        .findFirst();
  }

  private static CreateLicencePositionPayload createPayloadOf(LicencePositionStagedPayload positionStagedPayload) {
    return (CreateLicencePositionPayload) positionStagedPayload.getPayload();
  }

  private static Stream<LicencePositionStagedPayload> positionStagedPayloadsOfType(
      List<LicencePositionStagedPayload> positionStagedPayloads,
      LicencePositionStagedPayloadChangeType changeType
  ) {
    return positionStagedPayloads.stream()
        .filter(positionStagedPayload -> positionStagedPayload.getChangeType() == changeType);
  }
}