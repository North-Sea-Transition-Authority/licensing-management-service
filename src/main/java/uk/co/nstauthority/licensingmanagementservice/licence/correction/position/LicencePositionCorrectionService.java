package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;

@Service
public class LicencePositionCorrectionService {

  private final LicencePositionCorrectionRepository licencePositionCorrectionRepository;
  private final LicencePositionRepository licencePositionRepository;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionCorrectionService(
      LicencePositionCorrectionRepository licencePositionCorrectionRepository,
      LicencePositionRepository licencePositionRepository,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licencePositionCorrectionRepository = licencePositionCorrectionRepository;
    this.licencePositionRepository = licencePositionRepository;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public Optional<LicencePositionCorrection> findUpdatePositionCorrection(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return licencePositionCorrectionRepository.findByLicenceCorrectionAndTargetLicencePositionAndChangeType(
        licenceCorrection, licencePosition, LicencePositionCorrectionChangeType.UPDATE_POSITION);
  }

  @Transactional
  public LicencePositionCorrection save(LicencePositionCorrection positionCorrection) {
    return licencePositionCorrectionRepository.save(positionCorrection);
  }

  @Transactional
  public void delete(LicencePositionCorrection positionCorrection) {
    licencePositionCorrectionRepository.delete(positionCorrection);
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

  public List<SetEquityOperation> getCommittedSetEquityOperations(
      LicencePositionCorrection licencePositionCorrection
  ) {
    return setEquityOperations(licencePositionCorrection.getPayload().changes());
  }

  public List<SetEquityRow> getSetEquityViews(List<SetEquityOperation> operations) {
    var organisationIds = operations.stream()
        .map(SetEquityOperation::transferTo)
        .toList();

    var organisationNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationIds);

    return operations.stream()
        .map(operation -> new SetEquityRow(
            organisationNames.getOrDefault(operation.transferTo(), ""),
            operation.equity()
        ))
        .toList();
  }

  @Transactional
  public void commitSetEquity(
      LicencePositionCorrection licencePositionCorrection,
      List<SetEquityOperation> operations
  ) {
    applySetEquity(licencePositionCorrection, operations);
  }

  public LicencePositionCorrection getOrBuildUpdatePositionCorrection(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .orElseGet(() -> newUpdatePositionCorrection(licenceCorrection, licencePosition));
  }

  public List<SetEquityOperation> getCommittedSetEquityOperationsForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .map(this::getCommittedSetEquityOperations)
        .orElseGet(List::of);
  }

  @Transactional
  public void commitSetEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      List<SetEquityOperation> operations
  ) {
    var positionCorrection = getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    applySetEquity(positionCorrection, operations);
  }

  public LicencePositionCorrection newUpdatePositionCorrection(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    var positionCorrection = new LicencePositionCorrection();
    positionCorrection.setLicenceCorrection(licenceCorrection);
    positionCorrection.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);
    positionCorrection.setTargetLicencePosition(licencePosition);
    positionCorrection.setPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
        .withCorrectionReference(licenceCorrection.getCorrectionReference())
        .withChanges(List.of())
        .build());
    return positionCorrection;
  }

  public LicencePositionCorrection getPositionCorrectionContainingChange(
      LicenceCorrection licenceCorrection,
      String changeId
  ) {
    return licencePositionCorrectionRepository.findByLicenceCorrection(licenceCorrection)
        .stream()
        .filter(correction -> containsChange(correction.getPayload(), changeId))
        .findFirst()
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No position correction containing change with id %s found for licence correction %s"
                .formatted(changeId, licenceCorrection.getId())));
  }

  public List<OrderablePosition> getOrderableSameDatePositions(
      LicenceCorrection licenceCorrection,
      UUID positionId
  ) {
    var allOrderablePositions = OrderablePositionUtil.toOrderablePositions(loadCorrectionPositions(licenceCorrection));
    return OrderablePositionUtil.sameDatePositions(allOrderablePositions, positionId);
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

  public LicencePositionPayload withChanges(
      LicencePositionPayload payload,
      List<LicencePositionChangeType> changes
  ) {
    return switch (payload) {
      case CreateLicencePositionPayload create -> LicencePositionPayload.newCreateLicencePositionPayload()
          .withLicencePositionId(create.licencePositionId())
          .withLicenceTransactionId(create.licenceTransactionId())
          .withEffectiveDate(create.effectiveDate())
          .withEffectiveDateOrder(create.effectiveDateOrder())
          .withCorrectionReference(create.correctionReference())
          .withChanges(changes)
          .build();
      case UpdateLicencePositionPayload update -> LicencePositionPayload.newUpdateLicencePositionPayload()
          .withEffectiveDate(update.effectiveDate())
          .withEffectiveDateOrder(update.effectiveDateOrder())
          .withCorrectionReference(update.correctionReference())
          .withChanges(changes)
          .build();
    };
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

  private void applySetEquity(
      LicencePositionCorrection licencePositionCorrection,
      List<SetEquityOperation> operations
  ) {
    var payload = licencePositionCorrection.getPayload();

    var changes = payload.changes().stream()
        .filter(change -> !isSetEquityChange(change))
        .collect(Collectors.toCollection(ArrayList::new));

    if (!CollectionUtils.isEmpty(operations)) {
      changes.add(buildSetEquityChange(operations, nextChangeOrder(changes)));
    }

    licencePositionCorrection.setPayload(withChanges(payload, changes));
    licencePositionCorrectionRepository.save(licencePositionCorrection);
  }

  private List<SetEquityOperation> setEquityOperations(List<LicencePositionChangeType> changes) {
    return changes.stream()
        .filter(AddChange.class::isInstance)
        .map(AddChange.class::cast)
        .flatMap(change -> change.operations().stream())
        .filter(LicencePositionAddOperation.class::isInstance)
        .map(LicencePositionAddOperation.class::cast)
        .map(LicencePositionAddOperation::operation)
        .filter(SetEquityOperation.class::isInstance)
        .map(SetEquityOperation.class::cast)
        .toList();
  }

  private boolean isSetEquityChange(LicencePositionChangeType change) {
    return !setEquityOperations(List.of(change)).isEmpty();
  }

  private AddChange buildSetEquityChange(
      List<SetEquityOperation> operations,
      int changeOrder
  ) {
    var changeOperations = operations.stream()
        .map(operation -> (LicencePositionChangeOperation) LicencePositionChangeOperation.newLicencePositionAddOperation()
            .withOperationId(operation.id())
            .withOperation(operation)
            .build())
        .toList();

    return LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(changeOrder)
        .withOperations(changeOperations)
        .build();
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

  private static boolean containsChange(LicencePositionPayload payload, String changeId) {
    return payload.changes().stream().anyMatch(change -> changeId.equals(change.changeId()));
  }
}