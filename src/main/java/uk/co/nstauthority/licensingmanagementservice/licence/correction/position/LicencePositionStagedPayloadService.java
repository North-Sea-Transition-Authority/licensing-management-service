package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static java.util.function.Predicate.not;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.GeospatialLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

@Service
public class LicencePositionStagedPayloadService {

  private final LicencePositionStagedPayloadRepository licencePositionStagedPayloadRepository;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicencePositionService licencePositionService;

  public LicencePositionStagedPayloadService(
      LicencePositionStagedPayloadRepository licencePositionStagedPayloadRepository,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionService licencePositionService
  ) {
    this.licencePositionStagedPayloadRepository = licencePositionStagedPayloadRepository;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licencePositionService = licencePositionService;
  }

  public Optional<LicencePositionStagedPayload> findById(UUID licencePositionStagedPayloadId) {
    return licencePositionStagedPayloadRepository.findById(licencePositionStagedPayloadId);
  }

  public Optional<LicencePositionStagedPayload> findUpdatePositionStagedPayload(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return licencePositionStagedPayloadRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        licenceCorrection, licencePosition, LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
  }

  @Transactional
  public LicencePositionStagedPayload save(LicencePositionStagedPayload positionStagedPayload) {
    return licencePositionStagedPayloadRepository.save(positionStagedPayload);
  }

  @Transactional
  public void delete(LicencePositionStagedPayload positionStagedPayload) {
    licencePositionStagedPayloadRepository.delete(positionStagedPayload);
  }

  @Transactional
  public UUID addNewPosition(
      LicenceCorrection licenceCorrection,
      LocalDate positionDate,
      String correctionReference
  ) {
    return saveNewPosition(licenceCorrection, positionDate, correctionReference, UUID.randomUUID());
  }

  @Transactional
  public UUID addNewPosition(
      LicenceCorrection licenceCorrection,
      LocalDate positionDate,
      String correctionReference,
      UUID licenceTransactionId
  ) {
    return saveNewPosition(licenceCorrection, positionDate, correctionReference, licenceTransactionId);
  }

  private UUID saveNewPosition(
      LicenceCorrection licenceCorrection,
      LocalDate positionDate,
      String correctionReference,
      UUID licenceTransactionId
  ) {
    var newLicencePositionId = UUID.randomUUID().toString();
    var effectiveDateOrder = determineEffectiveDateOrder(licenceCorrection, positionDate);

    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(newLicencePositionId)
        .withLicenceTransactionId(licenceTransactionId.toString())
        .withEffectiveDate(positionDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .withCorrectionReference(correctionReference)
        .withChanges(List.of())
        .build();

    var positionStagedPayload = new LicencePositionStagedPayload();
    positionStagedPayload.setLicenceCorrection(licenceCorrection);
    positionStagedPayload.setChangeType(LicencePositionStagedPayloadChangeType.ADD_POSITION);
    positionStagedPayload.setTargetLicencePosition(null);
    positionStagedPayload.setPayload(payload);

    licencePositionStagedPayloadRepository.save(positionStagedPayload);

    return UUID.fromString(newLicencePositionId);
  }

  @Transactional
  public void removeExecutedPosition(LicenceCorrection licenceCorrection, LicencePosition licencePosition) {
    if (licencePosition.getStatus() != LicencePositionStatus.EXECUTED) {
      throw new IllegalStateException(
          "Cannot remove licence position %s as it is not executed"
              .formatted(licencePosition.getId()));
    }

    if (!canRemovePosition(licenceCorrection, licencePosition)) {
      throw new IllegalStateException(
          "Cannot remove licence position %s as it is already marked for removal in correction %s"
              .formatted(licencePosition.getId(), licenceCorrection.getId()));
    }

    licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection,
            licencePosition,
            LicencePositionStagedPayloadChangeType.UPDATE_POSITION
        )
        .ifPresent(licencePositionStagedPayloadRepository::delete);

    var licencePositionStagedPayload = new LicencePositionStagedPayload();
    licencePositionStagedPayload.setLicenceCorrection(licenceCorrection);
    licencePositionStagedPayload.setChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION);
    licencePositionStagedPayload.setTargetLicencePosition(licencePosition);
    licencePositionStagedPayloadRepository.save(licencePositionStagedPayload);
  }

  public boolean canRemovePosition(LicenceCorrection licenceCorrection, LicencePosition licencePosition) {
    return licencePosition.getStatus() == LicencePositionStatus.EXECUTED
        && !isPositionRemovedInCorrection(licenceCorrection, licencePosition);
  }

  public boolean isPositionRemovedInCorrection(LicenceCorrection licenceCorrection, LicencePosition licencePosition) {
    return licencePositionStagedPayloadRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection,
            licencePosition,
            LicencePositionStagedPayloadChangeType.REMOVE_POSITION
        );
  }

  @Transactional
  public void reinstateDeletedPositionStagedPayload(LicenceCorrection licenceCorrection, LicencePosition licencePosition) {
    if (!canReinstateDeletedPositionStagedPayload(licenceCorrection, licencePosition)) {
      throw new IllegalStateException(
          "Cannot reinstate licence position %s as it is not marked for removal in correction %s"
              .formatted(licencePosition.getId(), licenceCorrection.getId()));
    }

    var removePositionStagedPayload = licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection,
            licencePosition,
            LicencePositionStagedPayloadChangeType.REMOVE_POSITION
        )
        .orElseThrow(() -> new IllegalStateException(
            "No REMOVE_POSITION staged payload found for licence position %s in correction %s"
                .formatted(licencePosition.getId(), licenceCorrection.getId())));

    licencePositionStagedPayloadRepository.delete(removePositionStagedPayload);
  }

  public boolean canReinstateDeletedPositionStagedPayload(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return isPositionRemovedInCorrection(licenceCorrection, licencePosition);
  }

  @Transactional
  public void undoPositionStagedPayload(LicencePositionStagedPayload licencePositionStagedPayload) {
    licencePositionStagedPayloadRepository.delete(licencePositionStagedPayload);
  }

  public List<LicencePositionStagedPayload> getPositionStagedPayloads(LicenceCorrection licenceCorrection) {
    return licencePositionStagedPayloadRepository.findByLicenceCorrection(licenceCorrection);
  }

  public static Set<UUID> getRemovedPositionIds(List<LicencePositionStagedPayload> positionStagedPayloads) {
    return positionStagedPayloads.stream()
        .filter(positionStagedPayload ->
            LicencePositionStagedPayloadChangeType.REMOVE_POSITION.equals(positionStagedPayload.getChangeType()))
        .map(positionStagedPayload -> positionStagedPayload.getTargetLicencePosition().getId())
        .collect(Collectors.toSet());
  }

  public static List<LicencePositionStagedPayload> stagedPayloadsOfType(
      List<LicencePositionStagedPayload> positionStagedPayloads,
      LicencePositionStagedPayloadChangeType changeType
  ) {
    return positionStagedPayloads.stream()
        .filter(positionStagedPayload -> positionStagedPayload.getChangeType() == changeType)
        .toList();
  }

  public static Map<UUID, UpdateLicencePositionPayload> updatedPositionPayloadsByTargetId(
      List<LicencePositionStagedPayload> updatedStagedPayloads
  ) {
    return updatedStagedPayloads.stream()
        .collect(Collectors.toMap(
            positionStagedPayload -> positionStagedPayload.getTargetLicencePosition().getId(),
            positionStagedPayload -> (UpdateLicencePositionPayload) positionStagedPayload.getPayload()
        ));
  }

  public List<LicencePositionStagedPayload> getAddedLicencePositionStagedPayloads(LicenceCorrection licenceCorrection) {
    return licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndChangeType(licenceCorrection, LicencePositionStagedPayloadChangeType.ADD_POSITION);
  }

  public Optional<LicencePositionStagedPayload> findFirstAddedPositionStagedPayload(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId
  ) {
    return getAddedLicencePositionStagedPayloads(licenceCorrection)
        .stream()
        .filter(positionStagedPayload -> ((CreateLicencePositionPayload) positionStagedPayload.getPayload())
            .licencePositionId().equals(licencePositionId.toString()))
        .findFirst();
  }

  public boolean isCorrectionReferenceInUse(LicenceCorrection licenceCorrection, String correctionReference) {
    return getAddedLicencePositionStagedPayloads(licenceCorrection)
        .stream()
        .map(LicencePositionStagedPayload::getPayload)
        .filter(CreateLicencePositionPayload.class::isInstance)
        .map(CreateLicencePositionPayload.class::cast)
        .map(CreateLicencePositionPayload::correctionReference)
        .anyMatch(existingReference -> existingReference.equalsIgnoreCase(correctionReference));
  }

  @Transactional
  public LicencePositionStagedPayload correctPositionDate(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LocalDate correctPositionDate
  ) {
    var existingPositionStagedPayload = licencePositionStagedPayloadRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection, licencePosition, LicencePositionStagedPayloadChangeType.UPDATE_POSITION
        );

    var effectiveDateOrder = determineEffectiveDateOrder(licenceCorrection, correctPositionDate, licencePosition.getId());

    LicencePositionStagedPayload positionStagedPayload;
    UpdateLicencePositionPayload payload;

    if (existingPositionStagedPayload.isPresent()) {
      positionStagedPayload = existingPositionStagedPayload.get();
      var existingPayload = (UpdateLicencePositionPayload) positionStagedPayload.getPayload();

      payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withEffectiveDate(correctPositionDate)
          .withEffectiveDateOrder(effectiveDateOrder)
          .withCorrectionReference(existingPayload.correctionReference())
          .withChanges(existingPayload.changes())
          .build();
    } else {
      positionStagedPayload = new LicencePositionStagedPayload();
      positionStagedPayload.setLicenceCorrection(licenceCorrection);
      positionStagedPayload.setTargetLicencePosition(licencePosition);
      positionStagedPayload.setChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);

      payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withEffectiveDate(correctPositionDate)
          .withEffectiveDateOrder(effectiveDateOrder)
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of())
          .build();
    }

    positionStagedPayload.setPayload(payload);

    return licencePositionStagedPayloadRepository.save(positionStagedPayload);
  }

  public LicencePositionStagedPayload getOrBuildUpdatePositionStagedPayload(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return findUpdatePositionStagedPayload(licenceCorrection, licencePosition)
        .orElseGet(() -> newUpdatePositionStagedPayload(licenceCorrection, licencePosition));
  }

  /**
   * The position staged payload that changes to the position are staged on, if there is one yet: the staged position's own
   * ADD_POSITION staged payload, or the executed position's UPDATE_POSITION staged payload once the correction has
   * changed it.
   */
  public Optional<LicencePositionStagedPayload> findPositionStagedPayload(CorrectionPosition correctionPosition) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed(var licenceCorrection, var licencePosition) ->
          findUpdatePositionStagedPayload(licenceCorrection, licencePosition);
      case CorrectionPosition.Staged(var licencePositionStagedPayload) -> Optional.of(licencePositionStagedPayload);
    };
  }

  /**
   * The position staged payload that changes to the position are staged on: the staged position's own ADD_POSITION staged
   * payload, or the executed position's UPDATE_POSITION staged payload, built but not saved if the correction has not
   * changed it yet.
   */
  public LicencePositionStagedPayload getOrBuildPositionStagedPayload(CorrectionPosition correctionPosition) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed(var licenceCorrection, var licencePosition) ->
          getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition);
      case CorrectionPosition.Staged(var licencePositionStagedPayload) -> licencePositionStagedPayload;
    };
  }

  public LicencePositionStagedPayload getOrBuildPositionStagedPayload(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId
  ) {
    return findFirstAddedPositionStagedPayload(licenceCorrection, licencePositionId)
        .orElseGet(() -> getOrBuildUpdatePositionStagedPayload(
            licenceCorrection,
            licencePositionService.getPositionForLicence(licenceCorrection.getLicence(), licencePositionId)
        ));
  }

  @Transactional
  public LicencePositionStagedPayload stageChangesOnPosition(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      List<LicencePositionChangeType> changesToStage
  ) {
    var positionStagedPayload = getOrBuildPositionStagedPayload(licenceCorrection, licencePositionId);
    var payload = positionStagedPayload.getPayload();

    var changes = new ArrayList<>(payload.changes());
    changes.addAll(changesToStage);

    positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    return licencePositionStagedPayloadRepository.save(positionStagedPayload);
  }

  /**
   * Stages the removal of an executed change, replacing any correction this licence correction had already
   * staged against that same change. A staged change order for the removed change is left in place.
   */
  @Transactional
  public void stageRemovalOfExecutedChange(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      String changeId
  ) {
    var positionStagedPayload = getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition);
    var payload = positionStagedPayload.getPayload();

    var changes = new ArrayList<>(LicencePositionChangeType.removeChangesById(payload.changes(), changeId));
    changes.add(LicencePositionChangeType.removeChange().withChangeId(changeId).build());

    positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionStagedPayloadRepository.save(positionStagedPayload);
  }

  /**
   * Drops every change this licence correction has staged against an executed position that carries the operation
   * type, for operations a position holds at most one of.
   */
  @Transactional
  public void dropStagedChangesOf(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      Class<? extends LicenceOperation> operationType
  ) {
    findUpdatePositionStagedPayload(licenceCorrection, licencePosition).ifPresent(positionStagedPayload -> {
      var payload = positionStagedPayload.getPayload();
      var changes = LicencePositionChangeOperationUtil.removeChangesOf(payload.changes(), operationType);

      positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
      licencePositionStagedPayloadRepository.save(positionStagedPayload);
    });
  }

  /**
   * The date a change staged against this position staged payload takes effect. A partial surrender does
   * not record its own date on a staged payload - it takes the position date, so correcting that date
   * later moves the surrender with it.
   */
  public LocalDate resolveEffectiveDate(LicencePositionStagedPayload licencePositionStagedPayload) {
    return switch (licencePositionStagedPayload.getPayload()) {
      case CreateLicencePositionPayload create -> create.effectiveDate();
      case UpdateLicencePositionPayload update -> update.effectiveDate() != null
          ? update.effectiveDate()
          : targetPositionDate(licencePositionStagedPayload);
    };
  }

  private LocalDate targetPositionDate(LicencePositionStagedPayload licencePositionStagedPayload) {
    var targetLicencePosition = licencePositionStagedPayload.getTargetLicencePosition();

    if (targetLicencePosition == null) {
      throw new IllegalStateException(
          "Cannot resolve the effective date of licence position staged payload %s as it updates a position but has no target"
              .formatted(licencePositionStagedPayload.getId()));
    }

    return targetLicencePosition.getPositionDate();
  }

  public LocalDate getEffectivePositionDate(CorrectionPosition correctionPosition) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed(var licenceCorrection, var licencePosition) ->
          getEffectivePositionDate(licenceCorrection, licencePosition);
      case CorrectionPosition.Staged(var licencePositionStagedPayload) -> resolveEffectiveDate(licencePositionStagedPayload);
    };
  }

  public LocalDate getEffectivePositionDate(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return findUpdatePositionStagedPayload(licenceCorrection, licencePosition)
        .map(this::resolveEffectiveDate)
        .orElseGet(licencePosition::getPositionDate);
  }

  public LicencePositionStagedPayload newUpdatePositionStagedPayload(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    var positionStagedPayload = new LicencePositionStagedPayload();
    positionStagedPayload.setLicenceCorrection(licenceCorrection);
    positionStagedPayload.setChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    positionStagedPayload.setTargetLicencePosition(licencePosition);
    positionStagedPayload.setPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
        .withCorrectionReference(licenceCorrection.getCorrectionReference())
        .withChanges(List.of())
        .build());
    return positionStagedPayload;
  }

  public LicencePositionStagedPayload getPositionStagedPayloadContainingChange(
      LicenceCorrection licenceCorrection,
      String changeId
  ) {
    return licencePositionStagedPayloadRepository.findByLicenceCorrection(licenceCorrection)
        .stream()
        .filter(positionStagedPayload -> containsChange(positionStagedPayload, changeId))
        .findFirst()
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No position staged payload containing change with id %s found for licence correction %s"
                .formatted(changeId, licenceCorrection.getId())));
  }

  public Optional<LicencePositionChangeType> findStagedChange(
      LicencePositionStagedPayload positionStagedPayload,
      String changeId
  ) {
    return positionStagedPayload.getPayload().changes().stream()
        .filter(not(LicencePositionChangeType::isUpdateChangeOrder))
        .filter(change -> changeId.equals(change.changeId()))
        .findFirst();
  }

  public LicencePositionChangeType getStagedChangeOrThrow(
      LicencePositionStagedPayload positionStagedPayload,
      String changeId
  ) {
    return findStagedChange(positionStagedPayload, changeId)
        .orElseThrow(() -> new IllegalStateException(
            "No change with id %s found in position staged payload %s"
                .formatted(changeId, positionStagedPayload.getId())));
  }

  public List<LicenceOperation> resolveStagedChangeOperations(LicencePositionChangeType change) {
    if (change instanceof RemoveChange(String liveChangeId)) {
      return LicencePositionChange.operationsOf(
          licencePositionChangeService.getByIdOrThrow(UUID.fromString(liveChangeId)));
    }

    return LicencePositionChangeType.operationsOf(change);
  }

  public <T extends LicenceOperation> Optional<T> findStagedChangeOperation(
      LicencePositionChangeType change,
      Class<T> operationType
  ) {
    return resolveStagedChangeOperations(change).stream()
        .filter(operationType::isInstance)
        .map(operationType::cast)
        .findFirst();
  }

  @Transactional
  public void dropStagedChange(LicencePositionStagedPayload positionStagedPayload, String changeId) {
    var changes = positionStagedPayload.getPayload().changes();
    var remainingChanges = isLiveChangeMovedOnto(positionStagedPayload, changeId)
        ? LicencePositionChangeType.removeAllChangesById(changes, changeId)
        : LicencePositionChangeType.removeChangesById(changes, changeId);

    saveOrDiscardRemainingChanges(positionStagedPayload, remainingChanges);
  }

  @Transactional
  public void dropStagedChangeAndOrder(LicencePositionStagedPayload positionStagedPayload, String changeId) {
    saveOrDiscardRemainingChanges(
        positionStagedPayload,
        LicencePositionChangeType.removeAllChangesById(positionStagedPayload.getPayload().changes(), changeId)
    );
  }

  @Transactional
  public void saveOrDiscard(
      LicencePositionStagedPayload positionStagedPayload,
      List<LicencePositionChangeType> remainingChanges
  ) {
    saveOrDiscardRemainingChanges(positionStagedPayload, remainingChanges);
  }

  public List<OrderablePosition> getOrderableSameDatePositions(
      LicenceCorrection licenceCorrection,
      UUID positionId
  ) {
    var allOrderablePositions = OrderablePositionUtil.toOrderablePositions(loadCorrectionPositions(licenceCorrection));
    return OrderablePositionUtil.sameDatePositions(allOrderablePositions, positionId);
  }

  public List<OrderablePosition> getOrderablePositionsOnDate(
      LicenceCorrection licenceCorrection,
      LocalDate positionDate
  ) {
    return positionsOnDate(
        OrderablePositionUtil.toOrderablePositions(loadCorrectionPositions(licenceCorrection)),
        positionDate
    );
  }

  public SameTransactionPositionLookup findSameTransactionPositionOnDate(
      LicenceCorrection licenceCorrection,
      UUID sourcePositionId,
      LocalDate positionDate
  ) {
    var correctionPositions = loadCorrectionPositions(licenceCorrection);
    var transactionIdsByPositionId = transactionIdsByPositionId(correctionPositions);

    var transactionId = Optional.ofNullable(transactionIdsByPositionId.get(sourcePositionId))
        .orElseThrow(() -> new LmsEntityNotFoundException("licencePosition", sourcePositionId));

    var matchingPositionId = positionsOnDate(OrderablePositionUtil.toOrderablePositions(correctionPositions), positionDate)
        .stream()
        .map(OrderablePosition::id)
        .filter(positionId -> !positionId.equals(sourcePositionId))
        .filter(positionId -> transactionId.equals(transactionIdsByPositionId.get(positionId)))
        .findFirst()
        .orElse(null);

    return new SameTransactionPositionLookup(transactionId, matchingPositionId);
  }

  public List<OrderablePosition> getOrderableDatePositions(
      LicenceCorrection licenceCorrection
  ) {
    return OrderablePositionUtil.toOrderablePositions(loadCorrectionPositions(licenceCorrection))
        .stream()
        .sorted(Comparator.comparing(OrderablePosition::effectiveDate)
            .thenComparingInt(OrderablePosition::effectiveDateOrder))
        .toList();
  }

  public OrderablePosition getOrderableDatePosition(
      LicenceCorrection licenceCorrection,
      UUID positionId
  ) {
    return getOrderableDatePositions(licenceCorrection)
        .stream()
        .filter(position -> position.id().equals(positionId))
        .findFirst()
        .orElseThrow(() -> new LmsEntityNotFoundException("licencePosition", positionId));
  }

  @Transactional
  public void correctPositionOrder(
      LicenceCorrection licenceCorrection,
      UUID movedPositionId,
      UUID targetPositionId,
      PositionMoveDirection direction
  ) {
    var correctionPositions = loadCorrectionPositions(licenceCorrection);

    var positionIds = OrderablePositionUtil.sameDatePositions(
            OrderablePositionUtil.toOrderablePositions(correctionPositions), movedPositionId)
        .stream().map(OrderablePosition::id).toList();

    if (!positionIds.contains(movedPositionId)) {
      throw new IllegalArgumentException(
          "Cannot move position %s as it is not orderable on the same date".formatted(movedPositionId));
    }
    if (movedPositionId.equals(targetPositionId) || !positionIds.contains(targetPositionId)) {
      throw new IllegalArgumentException(
          "Cannot move position %s relative to position %s as they are not orderable on the same date"
              .formatted(movedPositionId, targetPositionId));
    }

    var reorderedIds = PositionOrderingUtil.moveRelativeTo(positionIds, movedPositionId, targetPositionId, direction);

    var executedPositionsById = correctionPositions.executedPositions()
        .stream()
        .collect(Collectors.toMap(LicencePosition::getId, Function.identity()));
    var addedStagedPayloadsByPositionId = correctionPositions.addStagedPayloads()
        .stream()
        .collect(Collectors.toMap(
            positionStagedPayload -> UUID.fromString(
                ((CreateLicencePositionPayload) positionStagedPayload.getPayload()).licencePositionId()),
            Function.identity()));
    var updateStagedPayloadsByPositionId = correctionPositions.updateStagedPayloads()
        .stream()
        .collect(Collectors.toMap(
            positionStagedPayload -> positionStagedPayload.getTargetLicencePosition().getId(),
            Function.identity()));

    for (var index = 0; index < reorderedIds.size(); index++) {
      var positionId = reorderedIds.get(index);
      var newOrder = index + 1;
      var executedPosition = executedPositionsById.get(positionId);
      if (executedPosition != null) {
        upsertPositionOrder(
            licenceCorrection, executedPosition, updateStagedPayloadsByPositionId.get(positionId), newOrder);
      } else {
        updateAddedPositionOrder(addedStagedPayloadsByPositionId.get(positionId), newOrder);
      }
    }
  }

  public int nextChangeOrder(List<LicencePositionChangeType> changes) {
    return changes.stream()
        .filter(AddChange.class::isInstance)
        .map(AddChange.class::cast)
        .map(AddChange::changeOrder)
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(0) + 1;
  }

  @Transactional
  public LicencePositionStagedPayload replaceAddChangeFor(
      LicencePositionStagedPayload licencePositionStagedPayload,
      Class<? extends LicenceOperation> operationType,
      List<? extends LicenceOperation> operations
  ) {
    return replaceAddChange(
        licencePositionStagedPayload,
        operationType,
        operation -> true,
        operations,
        this::nextChangeOrder
    );
  }

  @Transactional
  public <T extends LicenceOperation> LicencePositionStagedPayload replaceAddChangeFor(
      LicencePositionStagedPayload licencePositionStagedPayload,
      Class<T> operationType,
      Predicate<T> operationToReplace,
      List<? extends LicenceOperation> operations
  ) {
    return replaceAddChange(
        licencePositionStagedPayload,
        operationType,
        operationToReplace,
        operations,
        this::nextChangeOrder
    );
  }

  /**
   * As {@link #replaceAddChangeFor(LicencePositionStagedPayload, Class, Predicate, List)}, but the new change is ordered
   * after the executed position's live changes as well as its staged changes, so it is applied after any live change
   * its operations depend on.
   */
  @Transactional
  public <T extends LicenceOperation> LicencePositionStagedPayload replaceAddChangeForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionStagedPayload updateStagedPayload,
      Class<T> operationType,
      Predicate<T> operationToReplace,
      List<? extends LicenceOperation> operations
  ) {
    var liveChanges = getLiveChangesForExecutedPosition(licenceCorrection, licencePosition);

    return replaceAddChange(
        updateStagedPayload,
        operationType,
        operationToReplace,
        operations,
        remainingChanges -> nextChangeOrderAfter(PositionChange.foldChanges(liveChanges, remainingChanges))
    );
  }

  /**
   * Stages an add change for each list of operations, each ordered after the last, saving the position staged payload once.
   */
  @Transactional
  public LicencePositionStagedPayload addChanges(
      LicencePositionStagedPayload licencePositionStagedPayload,
      List<? extends List<? extends LicenceOperation>> operationsPerChange
  ) {
    return appendAddChanges(
        licencePositionStagedPayload,
        licencePositionStagedPayload.getPayload().changes(),
        operationsPerChange,
        this::nextChangeOrder
    );
  }

  /**
   * Stages an add change for each list of operations, each ordered after the last, but the new changes are ordered after the
   * executed position's live changes as well as its staged changes.
   */
  @Transactional
  public void addChangesForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionStagedPayload updateStagedPayload,
      List<? extends List<? extends LicenceOperation>> operationsPerChange
  ) {
    var liveChanges = getLiveChangesForExecutedPosition(licenceCorrection, licencePosition);

    appendAddChanges(
        updateStagedPayload,
        updateStagedPayload.getPayload().changes(),
        operationsPerChange,
        changes -> nextChangeOrderAfter(PositionChange.foldChanges(liveChanges, changes))
    );
  }

  private <T extends LicenceOperation> LicencePositionStagedPayload replaceAddChange(
      LicencePositionStagedPayload licencePositionStagedPayload,
      Class<T> operationType,
      Predicate<T> operationToReplace,
      List<? extends LicenceOperation> operations,
      ToIntFunction<List<LicencePositionChangeType>> nextChangeOrderFor
  ) {
    var remainingChanges = licencePositionStagedPayload.getPayload().changes().stream()
        .filter(change -> getAddOperationsOfType(List.of(change), operationType).stream().noneMatch(operationToReplace))
        .toList();

    return appendAddChanges(
        licencePositionStagedPayload,
        remainingChanges,
        Collections.singletonList(operations),
        nextChangeOrderFor
    );
  }

  private LicencePositionStagedPayload appendAddChanges(
      LicencePositionStagedPayload licencePositionStagedPayload,
      List<LicencePositionChangeType> existingChanges,
      List<? extends List<? extends LicenceOperation>> operationsPerChange,
      ToIntFunction<List<LicencePositionChangeType>> nextChangeOrderFor
  ) {
    var changes = new ArrayList<>(existingChanges);

    for (var operations : operationsPerChange) {
      if (!CollectionUtils.isEmpty(operations)) {
        changes.add(AddChange.buildOperationsChange(operations, nextChangeOrderFor.applyAsInt(changes)));
      }
    }

    var payload = licencePositionStagedPayload.getPayload();
    licencePositionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    return licencePositionStagedPayloadRepository.save(licencePositionStagedPayload);
  }

  private static int nextChangeOrderAfter(List<PositionChange> changes) {
    return changes.stream()
        .map(PositionChange::changeOrder)
        .filter(Objects::nonNull)
        // a staged change with no live change to take its order from is folded with this placeholder
        .filter(changeOrder -> changeOrder != Integer.MAX_VALUE)
        .max(Integer::compareTo)
        .orElse(0) + 1;
  }

  public <T extends LicenceOperation> List<T> getAddOperationsOfType(
      List<LicencePositionChangeType> changes,
      Class<T> operationType
  ) {
    return changes.stream()
        .filter(AddChange.class::isInstance)
        .map(AddChange.class::cast)
        .flatMap(change -> change.operations().stream())
        .filter(LicencePositionAddOperation.class::isInstance)
        .map(LicencePositionAddOperation.class::cast)
        .map(LicencePositionAddOperation::operation)
        .filter(operationType::isInstance)
        .map(operationType::cast)
        .toList();
  }

  public <T extends LicenceOperation> Optional<T> getCommittedChangeOfType(
      @Nullable LicencePositionStagedPayload licencePositionStagedPayload,
      Class<T> operationType
  ) {
    if (licencePositionStagedPayload == null) {
      return Optional.empty();
    }

    return LicencePositionChangeOperationUtil.findOperations(
            licencePositionStagedPayload.getPayload().changes(), operationType)
        .stream()
        .findFirst();
  }

  //TODO - LMS2-202: There are occurrences in the codebase when all positions are fetched when the change
  // information for only one is needed. In the future, methods could be refactored to use these instead.
  public List<PositionChange> getChangesForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      @Nullable LicencePositionStagedPayload updateStagedPayload
  ) {
    var liveChanges = getLiveChangesForExecutedPosition(licenceCorrection, licencePosition);
    var stagedChanges = updateStagedPayload == null ? List.<LicencePositionChangeType>of()
        : updateStagedPayload.getPayload().changes();

    return PositionChange.foldChanges(liveChanges, stagedChanges);
  }

  private List<LicencePositionChange> getLiveChangesForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return withoutMovedAwayChanges(
        licencePositionChangeService.findByLicencePositionId(licencePosition.getId()),
        licencePosition.getId(),
        getUpdatedChangePositionIds(licencePositionStagedPayloadRepository.findByLicenceCorrection(licenceCorrection))
    );
  }

  public List<PositionChange> getChangesForAddedPosition(LicencePositionStagedPayload addedStagedPayload) {
    return PositionChange.fromPayload(addedStagedPayload.getPayload());
  }

  public static Map<String, UUID> getUpdatedChangePositionIds(
      Collection<LicencePositionStagedPayload> positionStagedPayloads
  ) {
    var positionIdsByChangeId = new HashMap<String, UUID>();

    positionStagedPayloads.stream()
        .filter(positionStagedPayload -> positionStagedPayload.getPayload() != null)
        .forEach(positionStagedPayload -> positionStagedPayload.getPayload()
            .changesOfType(UpdateChangeOperations.class)
            .forEach(change -> positionIdsByChangeId.put(change.changeId(), positionStagedPayload.getPositionId())));

    return positionIdsByChangeId;
  }

  public static List<LicencePositionChange> withoutMovedAwayChanges(
      List<LicencePositionChange> liveChanges,
      UUID licencePositionId,
      Map<String, UUID> updatedChangePositionIds
  ) {
    return liveChanges.stream()
        .filter(liveChange -> !isMovedAway(liveChange.getId().toString(), licencePositionId, updatedChangePositionIds))
        .toList();
  }

  public static boolean isMovedAway(
      String changeId,
      UUID licencePositionId,
      Map<String, UUID> updatedChangePositionIds
  ) {
    var stagedOnPositionId = updatedChangePositionIds.get(changeId);
    return stagedOnPositionId != null && !stagedOnPositionId.equals(licencePositionId);
  }

  public boolean hasPendingChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      Class<? extends LicenceOperation> operationType
  ) {
    return findUpdatePositionStagedPayload(licenceCorrection, licencePosition)
        .map(positionStagedPayload ->
            LicencePositionChangeOperationUtil
                .changeExists(positionStagedPayload.getPayload().changes(), operationType))
        .orElse(false);
  }

  public Set<UUID> blockFeatureIdsAlreadyOperatedOn(CorrectionPosition correctionPosition) {
    return blockFeatureIdsAlreadyOperatedOn(correctionPosition, null);
  }

  public Set<UUID> blockFeatureIdsAlreadyOperatedOn(
      CorrectionPosition correctionPosition,
      @Nullable String changeIdToExclude
  ) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed(var licenceCorrection, var licencePosition) ->
          blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
              licenceCorrection,
              licencePosition,
              findUpdatePositionStagedPayload(licenceCorrection, licencePosition).orElse(null),
              changeIdToExclude
          );
      case CorrectionPosition.Staged(var licencePositionStagedPayload) ->
          blockFeatureIdsAlreadyOperatedOnForAddedPosition(licencePositionStagedPayload, changeIdToExclude);
    };
  }

  public Set<UUID> blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      @Nullable LicencePositionStagedPayload updateStagedPayload
  ) {
    return blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licenceCorrection,
        licencePosition,
        updateStagedPayload,
        null
    );
  }

  public Set<UUID> blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      @Nullable LicencePositionStagedPayload updateStagedPayload,
      @Nullable String changeIdToExclude
  ) {
    return featureIdsFromFold(
        getChangesForExecutedPosition(licenceCorrection, licencePosition, updateStagedPayload),
        changeIdToExclude
    );
  }

  public Set<UUID> blockFeatureIdsAlreadyOperatedOnForAddedPosition(
      LicencePositionStagedPayload addedStagedPayload
  ) {
    return blockFeatureIdsAlreadyOperatedOnForAddedPosition(addedStagedPayload, null);
  }

  public Set<UUID> blockFeatureIdsAlreadyOperatedOnForAddedPosition(
      LicencePositionStagedPayload addedStagedPayload,
      @Nullable String changeIdToExclude
  ) {
    return featureIdsFromFold(getChangesForAddedPosition(addedStagedPayload), changeIdToExclude);
  }

  private static Set<UUID> featureIdsFromFold(List<PositionChange> changes, @Nullable String changeIdToExclude) {
    return changes.stream()
        .filter(positionChange -> !LicencePositionChangeType.REMOVE_CHANGE.equals(positionChange.changeType()))
        .filter(positionChange -> !positionChange.changeId().equals(changeIdToExclude))
        .flatMap(positionChange -> positionChange.operations().stream())
        .filter(GeospatialLicenceOperation.class::isInstance)
        .map(GeospatialLicenceOperation.class::cast)
        .flatMap(geospatialLicenceOperation -> geospatialLicenceOperation.featureIds().stream())
        .collect(Collectors.toSet());
  }

  private void updateAddedPositionOrder(LicencePositionStagedPayload addedStagedPayload, int newOrder) {
    var payload = (CreateLicencePositionPayload) addedStagedPayload.getPayload();
    if (payload.effectiveDateOrder() == newOrder) {
      return;
    }
    addedStagedPayload.setPayload(LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(payload.licencePositionId())
        .withLicenceTransactionId(payload.licenceTransactionId())
        .withEffectiveDate(payload.effectiveDate())
        .withEffectiveDateOrder(newOrder)
        .withCorrectionReference(payload.correctionReference())
        .withChanges(payload.changes())
        .build());
    licencePositionStagedPayloadRepository.save(addedStagedPayload);
  }

  private void upsertPositionOrder(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionStagedPayload existingUpdateStagedPayload,
      int newOrder
  ) {
    var orderMatchesLive = newOrder == licencePosition.getPositionDateOrder();

    if (existingUpdateStagedPayload != null) {
      var existingPayload = (UpdateLicencePositionPayload) existingUpdateStagedPayload.getPayload();
      var effectiveDate = existingPayload.effectiveDate() != null
          ? existingPayload.effectiveDate() : licencePosition.getPositionDate();
      var dateMatchesLive = effectiveDate.equals(licencePosition.getPositionDate());
      var carriesOtherStagedChanges =
          existingPayload.correctionReference() != null || !existingPayload.changes().isEmpty();

      if (orderMatchesLive && dateMatchesLive && !carriesOtherStagedChanges) {
        licencePositionStagedPayloadRepository.delete(existingUpdateStagedPayload);
      } else {
        existingUpdateStagedPayload.setPayload(withUpdatedOrder(existingPayload, effectiveDate, newOrder));
        licencePositionStagedPayloadRepository.save(existingUpdateStagedPayload);
      }
    } else if (!orderMatchesLive) {
      var positionStagedPayload = new LicencePositionStagedPayload();
      positionStagedPayload.setLicenceCorrection(licenceCorrection);
      positionStagedPayload.setTargetLicencePosition(licencePosition);
      positionStagedPayload.setChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
      positionStagedPayload.setPayload(buildUpdatePayload(licencePosition.getPositionDate(), newOrder));
      licencePositionStagedPayloadRepository.save(positionStagedPayload);
    }
  }

  private UpdateLicencePositionPayload buildUpdatePayload(LocalDate effectiveDate, int effectiveDateOrder) {
    return LicencePositionPayload.newUpdateLicencePositionPayload()
        .withEffectiveDate(effectiveDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .withChanges(List.of())
        .build();
  }

  private UpdateLicencePositionPayload withUpdatedOrder(
      UpdateLicencePositionPayload existingPayload,
      LocalDate effectiveDate,
      int effectiveDateOrder
  ) {
    return LicencePositionPayload.newUpdateLicencePositionPayload()
        .withEffectiveDate(effectiveDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .withCorrectionReference(existingPayload.correctionReference())
        .withChanges(existingPayload.changes())
        .build();
  }

  private int determineEffectiveDateOrder(LicenceCorrection licenceCorrection, LocalDate positionDate) {
    return determineEffectiveDateOrder(licenceCorrection, positionDate, null);
  }

  private int determineEffectiveDateOrder(
      LicenceCorrection licenceCorrection,
      LocalDate positionDate,
      @Nullable UUID excludePositionId
  ) {
    var maxOrder = OrderablePositionUtil.toOrderablePositions(loadCorrectionPositions(licenceCorrection)).stream()
        .filter(position -> !position.id().equals(excludePositionId))
        .filter(position -> positionDate.equals(position.effectiveDate()))
        .mapToInt(OrderablePosition::effectiveDateOrder)
        .max()
        .orElse(0);

    return maxOrder + 1;
  }

  private static List<OrderablePosition> positionsOnDate(
      List<OrderablePosition> orderablePositions,
      LocalDate positionDate
  ) {
    return orderablePositions.stream()
        .filter(position -> position.effectiveDate().equals(positionDate))
        .sorted(Comparator.comparingInt(OrderablePosition::effectiveDateOrder))
        .toList();
  }

  private static Map<UUID, UUID> transactionIdsByPositionId(CorrectionPositions correctionPositions) {
    var transactionIdsByPositionId = new HashMap<UUID, UUID>();

    correctionPositions.executedPositions().forEach(position ->
        transactionIdsByPositionId.put(position.getId(), position.getLicenceTransaction().getId()));

    correctionPositions.addStagedPayloads().stream()
        .map(positionStagedPayload -> (CreateLicencePositionPayload) positionStagedPayload.getPayload())
        .forEach(payload -> transactionIdsByPositionId.put(
            UUID.fromString(payload.licencePositionId()),
            UUID.fromString(payload.licenceTransactionId())
        ));

    return transactionIdsByPositionId;
  }

  private CorrectionPositions loadCorrectionPositions(LicenceCorrection licenceCorrection) {
    var changeTypeToPositionStagedPayloads = licencePositionStagedPayloadRepository.findByLicenceCorrection(licenceCorrection)
        .stream()
        .collect(Collectors.groupingBy(LicencePositionStagedPayload::getChangeType));

    var removedPositionIds = changeTypeToPositionStagedPayloads
        .getOrDefault(LicencePositionStagedPayloadChangeType.REMOVE_POSITION, List.of())
        .stream()
        .map(LicencePositionStagedPayload::getTargetLicencePosition)
        .map(LicencePosition::getId)
        .collect(Collectors.toSet());

    var executedPositions = licencePositionService.getExecutedChronologicalLicencePositions(licenceCorrection.getLicence());

    return new CorrectionPositions(
        executedPositions,
        changeTypeToPositionStagedPayloads.getOrDefault(LicencePositionStagedPayloadChangeType.ADD_POSITION, List.of()),
        changeTypeToPositionStagedPayloads.getOrDefault(LicencePositionStagedPayloadChangeType.UPDATE_POSITION, List.of()),
        removedPositionIds
    );
  }

  private void saveOrDiscardRemainingChanges(
      LicencePositionStagedPayload positionStagedPayload,
      List<LicencePositionChangeType> remainingChanges
  ) {
    if (positionStagedPayload.getChangeType() == LicencePositionStagedPayloadChangeType.UPDATE_POSITION
        && remainingChanges.isEmpty()
        && LicencePositionChangeUtil.positionDateAndOrderUnchanged(positionStagedPayload)) {
      if (positionStagedPayload.getId() != null) {
        licencePositionStagedPayloadRepository.delete(positionStagedPayload);
      }
      return;
    }

    positionStagedPayload.setPayload(
        LicencePositionPayload.withChanges(positionStagedPayload.getPayload(), remainingChanges)
    );
    licencePositionStagedPayloadRepository.save(positionStagedPayload);
  }

  private boolean isLiveChangeMovedOnto(LicencePositionStagedPayload positionStagedPayload, String changeId) {
    return licencePositionChangeService.findById(UUID.fromString(changeId))
        .filter(liveChange -> !liveChange.getLicencePosition().getId().equals(positionStagedPayload.getPositionId()))
        .isPresent();
  }

  private boolean containsChange(LicencePositionStagedPayload licencePositionStagedPayload, String changeId) {
    var payload = licencePositionStagedPayload.getPayload();

    return payload != null
        && payload.changes().stream().anyMatch(change -> changeId.equals(change.changeId()));
  }
}
