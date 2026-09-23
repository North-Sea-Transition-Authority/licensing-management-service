package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicencePositionValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;

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

  public CorrectionApplyService(
      CorrectedTimelineService correctedTimelineService,
      LicencePositionValidationService licencePositionValidationService,
      LicenceCorrectionService licenceCorrectionService,
      LicencePositionChangeService licencePositionChangeService,
      LicenceTransactionService licenceTransactionService,
      LicencePositionRepository licencePositionRepository,
      LicencePositionChangeRepository licencePositionChangeRepository,
      LicenceTransactionRepository licenceTransactionRepository,
      EntityManager entityManager
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
  }

  @Transactional
  public List<PositionValidationError> applyCorrection(LicenceCorrection correction) {
    var licenceCorrection =
        entityManager.find(LicenceCorrection.class, correction.getId(), LockModeType.PESSIMISTIC_WRITE);

    if (licenceCorrection.getStatus() != LicenceCorrectionStatus.IN_PROGRESS) {
      throw new IllegalStateException(
          "Cannot apply licence correction %s as it is %s"
              .formatted(licenceCorrection.getId(), licenceCorrection.getStatus()));
    }

    var correctedTimeline = correctedTimelineService.getCorrectedTimeline(licenceCorrection);
    var blockingErrors = licencePositionValidationService.validate(
        correctedTimeline.positionsToApply(),
        correctedTimeline.resolvedStates(),
        correctedTimeline.isCarbonStorage()
    );

    if (!blockingErrors.isEmpty()) {
      return blockingErrors;
    }

    var positionCorrections = correctedTimeline.positionCorrections();

    positionCorrectionsOfType(positionCorrections, LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .forEach(this::updatePosition);

    var addedPositionsById = new HashMap<UUID, LicencePosition>();
    positionCorrectionsOfType(positionCorrections, LicencePositionCorrectionChangeType.ADD_POSITION)
        .forEach(positionCorrection -> addedPositionsById.put(
            positionCorrection.getPositionId(),
            addPosition(licenceCorrection, positionCorrection)));

    positionCorrections.stream()
        .filter(positionCorrection ->
            positionCorrection.getChangeType() != LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .forEach(positionCorrection -> applyStagedChanges(positionCorrection, addedPositionsById));

    positionCorrectionsOfType(positionCorrections, LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .forEach(this::removePosition);

    licenceCorrectionService.completeCorrection(licenceCorrection);

    LOGGER.info(
        "Applied licence correction {} ({}) to licence {} with {} position corrections",
        licenceCorrection.getId(),
        licenceCorrection.getCorrectionReference(),
        licenceCorrection.getLicence().getId(),
        positionCorrections.size()
    );

    return List.of();
  }

  private void updatePosition(LicencePositionCorrection positionCorrection) {
    var payload = (UpdateLicencePositionPayload) positionCorrection.getPayload();
    var licencePosition = positionCorrection.getTargetLicencePosition();

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
      LicencePositionCorrection positionCorrection
  ) {
    var payload = createPayloadOf(positionCorrection);
    var licence = licenceCorrection.getLicence();
    var licenceTransactionId = UUID.fromString(payload.licenceTransactionId());
    var correctionReference = Objects.requireNonNullElse(
        payload.correctionReference(), licenceCorrection.getCorrectionReference());

    var licenceTransaction = licenceTransactionRepository.findById(licenceTransactionId)
        .orElseGet(() -> licenceTransactionService.createLicenceTransaction(
            licenceTransactionId, correctionReference));

    var licencePosition = new LicencePosition(positionCorrection.getPositionId());
    licencePosition.setLicence(licence);
    licencePosition.setLicenceTransaction(licenceTransaction);
    licencePosition.setPositionDate(payload.effectiveDate());
    licencePosition.setPositionDateOrder(payload.effectiveDateOrder());
    licencePosition.setStatus(LicencePositionStatus.EXECUTED);

    entityManager.persist(licencePosition);

    return licencePosition;
  }

  private void removePosition(LicencePositionCorrection positionCorrection) {
    var licencePosition = positionCorrection.getTargetLicencePosition();
    licencePosition.setStatus(LicencePositionStatus.REMOVED);

    licencePositionRepository.save(licencePosition);
  }

  private void applyStagedChanges(
      LicencePositionCorrection positionCorrection,
      Map<UUID, LicencePosition> addedPositionsById
  ) {
    var payload = positionCorrection.getPayload();
    var licencePosition = resolvePosition(positionCorrection, addedPositionsById);

    payload.changesOfType(RemoveChange.class).forEach(this::removeChange);
    payload.changesOfType(AddChange.class).forEach(change -> addChange(licencePosition, change));
    payload.changesOfType(UpdateChangeOperations.class)
        .forEach(change -> updateChangeOperations(licencePosition, payload, change));
    payload.changesOfType(UpdateChangeOrder.class).forEach(this::updateChangeOrder);
  }

  private void removeChange(RemoveChange change) {
    licencePositionChangeService.findById(UUID.fromString(change.changeId()))
        .ifPresent(licencePositionChangeRepository::delete);
  }

  private void addChange(LicencePosition licencePosition, AddChange change) {
    licencePositionChangeService.createLicencePositionChange(
        UUID.fromString(change.changeId()),
        licencePosition,
        LicencePositionChangeType.operationsOf(change),
        change.changeOrder(),
        APPLIED_CHANGE_STATUS
    );
  }

  private void updateChangeOperations(
      LicencePosition licencePosition,
      LicencePositionPayload payload,
      UpdateChangeOperations change
  ) {
    var changeId = UUID.fromString(change.changeId());
    var operations = LicencePositionChangeType.operationsOf(change);

    licencePositionChangeService.findById(changeId).ifPresentOrElse(
        licencePositionChange -> {
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

  private void updateChangeOrder(UpdateChangeOrder change) {
    licencePositionChangeService.findById(UUID.fromString(change.changeId()))
        .ifPresent(licencePositionChange -> {
          licencePositionChange.setChangeOrder(change.changeOrder());
          licencePositionChangeRepository.save(licencePositionChange);
        });
  }

  private static LicencePosition resolvePosition(
      LicencePositionCorrection positionCorrection,
      Map<UUID, LicencePosition> addedPositionsById
  ) {
    return switch (positionCorrection.getPayload()) {
      case CreateLicencePositionPayload ignored -> addedPositionsById.get(positionCorrection.getPositionId());
      case UpdateLicencePositionPayload ignored -> positionCorrection.getTargetLicencePosition();
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

  private static CreateLicencePositionPayload createPayloadOf(LicencePositionCorrection positionCorrection) {
    return (CreateLicencePositionPayload) positionCorrection.getPayload();
  }

  private static Stream<LicencePositionCorrection> positionCorrectionsOfType(
      List<LicencePositionCorrection> positionCorrections,
      LicencePositionCorrectionChangeType changeType
  ) {
    return positionCorrections.stream()
        .filter(positionCorrection -> positionCorrection.getChangeType() == changeType);
  }
}