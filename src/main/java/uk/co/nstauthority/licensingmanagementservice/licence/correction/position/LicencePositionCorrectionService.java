package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionUpdateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;

@Service
public class LicencePositionCorrectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LicencePositionCorrectionService.class);
  private final LicencePositionCorrectionRepository licencePositionCorrectionRepository;
  private final LicencePositionRepository licencePositionRepository;

  public LicencePositionCorrectionService(
      LicencePositionCorrectionRepository licencePositionCorrectionRepository,
      LicencePositionRepository licencePositionRepository
  ) {
    this.licencePositionCorrectionRepository = licencePositionCorrectionRepository;
    this.licencePositionRepository = licencePositionRepository;
  }

  @Transactional
  public void addNewPosition(
      LicenceCorrection licenceCorrection,
      LocalDate positionDate,
      String correctionReference
  ) {
    var newLicencePositionId = UUID.randomUUID().toString();
    var newLicenceTransactionId = UUID.randomUUID().toString();
    var effectiveDateOrder = determineEffectiveDateOrder(licenceCorrection, positionDate);

    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(newLicencePositionId)
        .withLicenceTransactionId(newLicenceTransactionId)
        .withEffectiveDate(positionDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .withCorrectionReference(correctionReference)
        .withChanges(List.of())
        .build();

    var licenceCorrectionPosition = new LicencePositionCorrection();
    licenceCorrectionPosition.setLicenceCorrection(licenceCorrection);
    licenceCorrectionPosition.setChangeType(LicencePositionCorrectionChangeType.ADD_POSITION);
    licenceCorrectionPosition.setTargetLicencePosition(null);
    licenceCorrectionPosition.setPayload(payload);

    licencePositionCorrectionRepository.save(licenceCorrectionPosition);
  }

  @Transactional
  public void removeExecutedPosition(LicenceCorrection licenceCorrection, LicencePosition licencePosition) {
    if (!licencePosition.isExecuted()) {
      throw new IllegalStateException(
          "Cannot remove licence position %s as it is not executed"
              .formatted(licencePosition.getId()));
    }

    if (!canRemovePosition(licenceCorrection, licencePosition)) {
      throw new IllegalStateException(
          "Cannot remove licence position %s as it is already marked for removal in correction %s"
              .formatted(licencePosition.getId(), licenceCorrection.getId()));
    }

    licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection,
            licencePosition,
            LicencePositionCorrectionChangeType.UPDATE_POSITION
        )
        .ifPresent(licencePositionCorrectionRepository::delete);

    var licencePositionCorrection = new LicencePositionCorrection();
    licencePositionCorrection.setLicenceCorrection(licenceCorrection);
    licencePositionCorrection.setChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION);
    licencePositionCorrection.setTargetLicencePosition(licencePosition);
    licencePositionCorrectionRepository.save(licencePositionCorrection);
  }

  public boolean canRemovePosition(LicenceCorrection licenceCorrection, LicencePosition licencePosition) {
    return licencePosition.isExecuted()
        && !licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection,
            licencePosition,
            LicencePositionCorrectionChangeType.REMOVE_POSITION
        );
  }

  @Transactional
  public void reinstateDeletedPositionCorrection(LicenceCorrection licenceCorrection, LicencePosition licencePosition) {
    if (!canReinstateDeletedPositionCorrection(licenceCorrection, licencePosition)) {
      throw new IllegalStateException(
          "Cannot reinstate licence position %s as it is not marked for removal in correction %s"
              .formatted(licencePosition.getId(), licenceCorrection.getId()));
    }

    var removePositionCorrection = licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection,
            licencePosition,
            LicencePositionCorrectionChangeType.REMOVE_POSITION
        )
        .orElseThrow(() -> new IllegalStateException(
            "No REMOVE_POSITION correction found for licence position %s in correction %s"
                .formatted(licencePosition.getId(), licenceCorrection.getId())));

    licencePositionCorrectionRepository.delete(removePositionCorrection);
  }

  public boolean canReinstateDeletedPositionCorrection(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection,
            licencePosition,
            LicencePositionCorrectionChangeType.REMOVE_POSITION
        );
  }

  public LicencePositionCorrection getPositionCorrectionForCorrection(
      UUID licencePositionCorrectionId,
      LicenceCorrection licenceCorrection
  ) {
    return licencePositionCorrectionRepository
        .findByIdAndLicenceCorrection(licencePositionCorrectionId, licenceCorrection)
        .orElseThrow(() -> new LmsEntityNotFoundException("licencePositionCorrection", licencePositionCorrectionId));
  }

  @Transactional
  public void undoPositionCorrection(LicencePositionCorrection licencePositionCorrection) {
    licencePositionCorrectionRepository.delete(licencePositionCorrection);
  }

  public List<LicencePositionCorrection> getPositionCorrections(LicenceCorrection licenceCorrection) {
    return licencePositionCorrectionRepository.findByLicenceCorrection(licenceCorrection);
  }

  public List<LicencePositionCorrection> getAddedLicencePositionCorrections(LicenceCorrection licenceCorrection) {
    return licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(licenceCorrection, LicencePositionCorrectionChangeType.ADD_POSITION);
  }

  public boolean isCorrectionReferenceInUse(LicenceCorrection licenceCorrection, String correctionReference) {
    return getAddedLicencePositionCorrections(licenceCorrection)
        .stream()
        .map(LicencePositionCorrection::getPayload)
        .filter(CreateLicencePositionPayload.class::isInstance)
        .map(CreateLicencePositionPayload.class::cast)
        .map(CreateLicencePositionPayload::correctionReference)
        .anyMatch(existingReference -> existingReference.equalsIgnoreCase(correctionReference));
  }

  @Transactional
  public LicencePositionCorrection correctPositionDate(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LocalDate correctPositionDate
  ) {
    var existingPositionCorrection = licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION
        );

    var effectiveDateOrder = determineEffectiveDateOrder(licenceCorrection, correctPositionDate);

    LicencePositionCorrection positionCorrection;
    UpdateLicencePositionPayload payload;

    if (existingPositionCorrection.isPresent()) {
      positionCorrection = existingPositionCorrection.get();
      var existingPayload = (UpdateLicencePositionPayload) positionCorrection.getPayload();

      payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withEffectiveDate(correctPositionDate)
          .withEffectiveDateOrder(effectiveDateOrder)
          .withCorrectionReference(existingPayload.correctionReference())
          .withChanges(existingPayload.changes())
          .build();
    } else {
      positionCorrection = new LicencePositionCorrection();
      positionCorrection.setLicenceCorrection(licenceCorrection);
      positionCorrection.setTargetLicencePosition(licencePosition);
      positionCorrection.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);

      payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withEffectiveDate(correctPositionDate)
          .withEffectiveDateOrder(effectiveDateOrder)
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of())
          .build();
    }

    positionCorrection.setPayload(payload);

    return licencePositionCorrectionRepository.save(positionCorrection);
  }

  @Transactional
  public void addAdministratorChangeForAddedLicencePosition(
      LicencePositionCorrection licencePositionCorrection,
      Integer administratorId
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();

    if (adminChangeExists(payload.changes())) { //TODO LMS2-83: update change added in this correction
      LOGGER.warn("new admin change not applied for added licence position {} - already on payload", payload.licencePositionId());
      return;
    }

    var changes = new ArrayList<>(payload.changes());
    changes.add(AddChange.buildAddAdminChange(administratorId, changes.size() + 1));

    var updatedPayload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(payload.licencePositionId())
        .withLicenceTransactionId(payload.licenceTransactionId())
        .withEffectiveDate(payload.effectiveDate())
        .withEffectiveDateOrder(payload.effectiveDateOrder())
        .withCorrectionReference(payload.correctionReference())
        .withChanges(changes)
        .build();

    licencePositionCorrection.setPayload(updatedPayload);
    licencePositionCorrectionRepository.save(licencePositionCorrection);
  }

  @Transactional
  public void addAdministratorChangeForExistingLicencePosition(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      Integer administratorId
  ) {
    var existingPositionCorrection = licencePositionCorrectionRepository
        .findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection,
            licencePosition,
            LicencePositionCorrectionChangeType.UPDATE_POSITION
        );

    LicencePositionCorrection positionCorrection;

    if (existingPositionCorrection.isPresent()) {
      positionCorrection = existingPositionCorrection.get();
      var payload = (UpdateLicencePositionPayload) positionCorrection.getPayload();

      if (adminChangeExists(payload.changes())) { //TODO LMS2-83: update change added in this correction
        LOGGER.warn("new admin change not applied for licence position {} - already on payload", licencePosition.getId());
        return;
      }

      var changes = new ArrayList<>(payload.changes());
      changes.add(AddChange.buildAddAdminChange(administratorId, changes.size() + 1));

      var updatedPayload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withEffectiveDate(payload.effectiveDate())
          .withEffectiveDateOrder(payload.effectiveDateOrder())
          .withCorrectionReference(payload.correctionReference())
          .withChanges(changes)
          .build();

      positionCorrection.setPayload(updatedPayload);

    } else {
      positionCorrection = new LicencePositionCorrection();
      var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of(AddChange.buildAddAdminChange(administratorId, 1)))
          .build();

      positionCorrection.setLicenceCorrection(licenceCorrection);
      positionCorrection.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);
      positionCorrection.setTargetLicencePosition(licencePosition);
      positionCorrection.setPayload(payload);

    }

    licencePositionCorrectionRepository.save(positionCorrection);
  }

  @Transactional
  public void correctExistingAdministratorChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String originalChangeId,
      Integer administratorId
  ) {
    var existingPositionCorrection =
        licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
          licenceCorrection,
          licencePosition,
          LicencePositionCorrectionChangeType.UPDATE_POSITION
    );

    LicencePositionCorrection positionCorrection;

    if (existingPositionCorrection.isPresent()) {
      positionCorrection = existingPositionCorrection.get();
      var payload = (UpdateLicencePositionPayload) positionCorrection.getPayload();

      if (adminChangeExists(payload.changes())) { //TODO LMS2-83: update change added in this correction
        LOGGER.warn("update admin change not applied for licence position {} - already on payload", licencePosition.getId());
        return;
      }

      var changes = new ArrayList<>(payload.changes());
      changes.add(UpdateChangeOperations.buildUpdateAdminChange(originalChangeId, administratorId));

      var updatedPayload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withEffectiveDate(payload.effectiveDate())
          .withEffectiveDateOrder(payload.effectiveDateOrder())
          .withCorrectionReference(payload.correctionReference())
          .withChanges(changes)
          .build();

      positionCorrection.setPayload(updatedPayload);

    } else {
      positionCorrection = new LicencePositionCorrection();
      var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of(UpdateChangeOperations.buildUpdateAdminChange(originalChangeId, administratorId)))
          .build();

      positionCorrection.setLicenceCorrection(licenceCorrection);
      positionCorrection.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);
      positionCorrection.setTargetLicencePosition(licencePosition);
      positionCorrection.setPayload(payload);

    }

    licencePositionCorrectionRepository.save(positionCorrection);
  }

  public boolean adminChangeExists(List<LicencePositionChangeType> changes) {
    return changes.stream()
        .flatMap(change -> switch (change) {
          case AddChange addChange -> addChange.operations().stream();
          case UpdateChangeOperations updateChangeOperations -> updateChangeOperations.operations().stream();
        })
        .map(changeOperation -> switch (changeOperation) {
          case LicencePositionAddOperation addOperation -> addOperation.operation();
          case LicencePositionUpdateOperation updateOperation -> updateOperation.operation();
        })
        .anyMatch(AdministratorOperation.class::isInstance);
  }


  public List<OrderablePosition> getOrderableSameDatePositions(
      LicenceCorrection licenceCorrection,
      UUID positionId
  ) {
    var allOrderablePositions = OrderablePositionUtil.toOrderablePositions(loadCorrectionPositions(licenceCorrection));
    return OrderablePositionUtil.sameDatePositions(allOrderablePositions, positionId);
  }

  private CorrectionPositions loadCorrectionPositions(LicenceCorrection licenceCorrection) {
    var correctionsByChangeType = licencePositionCorrectionRepository.findByLicenceCorrection(licenceCorrection)
        .stream()
        .collect(Collectors.groupingBy(LicencePositionCorrection::getChangeType));

    var removedPositionIds = correctionsByChangeType
        .getOrDefault(LicencePositionCorrectionChangeType.REMOVE_POSITION, List.of())
        .stream()
        .map(LicencePositionCorrection::getTargetLicencePosition)
        .map(LicencePosition::getId)
        .collect(Collectors.toSet());

    var executedPositions = licencePositionRepository.findByLicence(licenceCorrection.getLicence())
        .stream()
        .filter(LicencePosition::isExecuted)
        .toList();

    return new CorrectionPositions(
        executedPositions,
        correctionsByChangeType.getOrDefault(LicencePositionCorrectionChangeType.ADD_POSITION, List.of()),
        correctionsByChangeType.getOrDefault(LicencePositionCorrectionChangeType.UPDATE_POSITION, List.of()),
        removedPositionIds
    );
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
    var addedCorrectionsByPositionId = correctionPositions.addCorrections()
        .stream()
        .collect(Collectors.toMap(
            correction -> UUID.fromString(((CreateLicencePositionPayload) correction.getPayload()).licencePositionId()),
            Function.identity()));
    var updateCorrectionsByPositionId = correctionPositions.updateCorrections()
        .stream()
        .collect(Collectors.toMap(
            correction -> correction.getTargetLicencePosition().getId(),
            Function.identity()));

    for (var index = 0; index < reorderedIds.size(); index++) {
      var positionId = reorderedIds.get(index);
      var newOrder = index + 1;
      var executedPosition = executedPositionsById.get(positionId);
      if (executedPosition != null) {
        upsertPositionOrder(
            licenceCorrection, executedPosition, updateCorrectionsByPositionId.get(positionId), newOrder);
      } else {
        updateAddedPositionOrder(addedCorrectionsByPositionId.get(positionId), newOrder);
      }
    }
  }

  private void updateAddedPositionOrder(LicencePositionCorrection addedCorrection, int newOrder) {
    var payload = (CreateLicencePositionPayload) addedCorrection.getPayload();
    if (payload.effectiveDateOrder() == newOrder) {
      return;
    }
    addedCorrection.setPayload(LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(payload.licencePositionId())
        .withLicenceTransactionId(payload.licenceTransactionId())
        .withEffectiveDate(payload.effectiveDate())
        .withEffectiveDateOrder(newOrder)
        .withCorrectionReference(payload.correctionReference())
        .withChanges(payload.changes())
        .build());
    licencePositionCorrectionRepository.save(addedCorrection);
  }

  private void upsertPositionOrder(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionCorrection existingUpdateCorrection,
      int newOrder
  ) {
    var orderMatchesLive = newOrder == licencePosition.getPositionDateOrder();

    if (existingUpdateCorrection != null) {
      var existingPayload = (UpdateLicencePositionPayload) existingUpdateCorrection.getPayload();
      var effectiveDate = existingPayload.effectiveDate() != null
          ? existingPayload.effectiveDate() : licencePosition.getPositionDate();
      var dateMatchesLive = effectiveDate.equals(licencePosition.getPositionDate());
      var carriesOtherCorrections =
          existingPayload.correctionReference() != null || !existingPayload.changes().isEmpty();

      if (orderMatchesLive && dateMatchesLive && !carriesOtherCorrections) {
        licencePositionCorrectionRepository.delete(existingUpdateCorrection);
      } else {
        existingUpdateCorrection.setPayload(withUpdatedOrder(existingPayload, effectiveDate, newOrder));
        licencePositionCorrectionRepository.save(existingUpdateCorrection);
      }
    } else if (!orderMatchesLive) {
      var positionCorrection = new LicencePositionCorrection();
      positionCorrection.setLicenceCorrection(licenceCorrection);
      positionCorrection.setTargetLicencePosition(licencePosition);
      positionCorrection.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);
      positionCorrection.setPayload(buildUpdatePayload(licencePosition.getPositionDate(), newOrder));
      licencePositionCorrectionRepository.save(positionCorrection);
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

  //TODO - Effective date order is auto-assigned for now. When multiple
  // positions share the same effective date the user should be able to select the order themselves.
  private int determineEffectiveDateOrder(LicenceCorrection licenceCorrection, LocalDate positionDate) {
    var liveMaxOrder = Optional.ofNullable(
        licencePositionRepository.findMaxPositionDateOrder(licenceCorrection.getLicence(), positionDate)
    ).orElse(0);

    var draftMaxOrder = licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(licenceCorrection, LicencePositionCorrectionChangeType.ADD_POSITION)
        .stream()
        .map(LicencePositionCorrection::getPayload)
        .filter(CreateLicencePositionPayload.class::isInstance)
        .map(CreateLicencePositionPayload.class::cast)
        .filter(payload -> positionDate.equals(payload.effectiveDate()))
        .map(CreateLicencePositionPayload::effectiveDateOrder)
        .max(Integer::compareTo)
        .orElse(0);

    return Math.max(liveMaxOrder, draftMaxOrder) + 1;
  }
}