package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;

@Service
public class TransferEquityCorrectionService {

  private final LicencePositionCorrectionRepository licencePositionCorrectionRepository;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final LicencePositionViewService licencePositionViewService;

  public TransferEquityCorrectionService(
      LicencePositionCorrectionRepository licencePositionCorrectionRepository,
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService,
      LicencePositionViewService licencePositionViewService
  ) {
    this.licencePositionCorrectionRepository = licencePositionCorrectionRepository;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.licencePositionViewService = licencePositionViewService;
  }

  public Map<Integer, BigDecimal> getEquityHoldingsForCorrection(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId
  ) {
    var chronologicalPositions = licencePositionViewService
        .getCorrectedChronologicalPositions(licenceCorrection, licencePositionId);
    return resolveEquityHoldings(licencePositionId, chronologicalPositions);
  }

  public Map<Integer, BigDecimal> getEquityHoldingsForAddedPosition(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
    return getEquityHoldingsForCorrection(licenceCorrection, UUID.fromString(payload.licencePositionId()));
  }

  private Map<Integer, BigDecimal> resolveEquityHoldings(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    var holdings = new HashMap<Integer, BigDecimal>();

    for (var chronologicalPosition : chronologicalPositions) {
      chronologicalPosition.changes().stream()
          .flatMap(change -> change.operations().stream())
          .forEach(operation -> applyEquityOperation(holdings, operation));

      if (chronologicalPosition.id().equals(currentLicencePositionId)) {
        break;
      }
    }

    return holdings;
  }

  private void applyEquityOperation(Map<Integer, BigDecimal> holdings, LicenceOperation operation) {
    if (operation instanceof SetEquityOperation(var transferTo, var equity)) {
      holdings.put(transferTo, equity);
    } else if (operation instanceof TransferEquityOperation(
        var transferFrom, var transferTo, var equity, var retainBeneficialInterest)) {
      holdings.merge(transferFrom, equity.negate(), BigDecimal::add);
      holdings.merge(transferTo, equity, BigDecimal::add);
    }
  }

  public List<TransferEquityOperation> getCommittedTransferEquityOperations(
      LicencePositionCorrection licencePositionCorrection
  ) {
    return transferEquityOperations(licencePositionCorrection.getPayload().changes());
  }

  @Transactional
  public void addTransferEquity(
      LicencePositionCorrection licencePositionCorrection,
      LicencePositionTransferEquityForm form
  ) {
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(licencePositionCorrection));
    operations.add(toTransferEquityOperation(form));
    applyTransferEquity(licencePositionCorrection, operations);
  }

  @Transactional
  public void setTransferEquityRetention(
      LicencePositionCorrection licencePositionCorrection,
      int index,
      boolean retainsBeneficialInterest
  ) {
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(licencePositionCorrection));
    if (isOutOfRange(operations, index)) {
      return;
    }
    operations.set(index, withRetention(operations.get(index), retainsBeneficialInterest));
    applyTransferEquity(licencePositionCorrection, operations);
  }

  @Transactional
  public void removeTransferEquity(LicencePositionCorrection licencePositionCorrection, int index) {
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(licencePositionCorrection));
    if (isOutOfRange(operations, index)) {
      return;
    }
    operations.remove(index);
    applyTransferEquity(licencePositionCorrection, operations);
  }

  public List<TransferEquityHoldingView> getTransferEquityViews(List<TransferEquityOperation> operations) {
    var organisationIds = operations.stream()
        .flatMap(operation -> Stream.of(operation.transferFrom(), operation.transferTo()))
        .distinct()
        .toList();

    var organisationNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationIds);

    return operations.stream()
        .map(operation -> new TransferEquityHoldingView(
            organisationNames.getOrDefault(operation.transferFrom(), ""),
            organisationNames.getOrDefault(operation.transferTo(), ""),
            operation.equity(),
            operation.retainBeneficialInterest()
        ))
        .toList();
  }

  public List<TransferEquityOperation> getCommittedTransferEquityOperationsForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .map(this::getCommittedTransferEquityOperations)
        .orElseGet(List::of);
  }

  @Transactional
  public void addTransferEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionTransferEquityForm form
  ) {
    var positionCorrection = licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .orElseGet(() -> licencePositionCorrectionService.newUpdatePositionCorrection(licenceCorrection, licencePosition));
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(positionCorrection));
    operations.add(toTransferEquityOperation(form));
    applyTransferEquity(positionCorrection, operations);
  }

  @Transactional
  public void setTransferEquityRetentionForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      int index,
      boolean retainsBeneficialInterest
  ) {
    var positionCorrection = licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .orElseGet(() -> licencePositionCorrectionService.newUpdatePositionCorrection(licenceCorrection, licencePosition));
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(positionCorrection));
    if (isOutOfRange(operations, index)) {
      return;
    }
    operations.set(index, withRetention(operations.get(index), retainsBeneficialInterest));
    applyTransferEquity(positionCorrection, operations);
  }

  @Transactional
  public void removeTransferEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      int index
  ) {
    var positionCorrection = licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .orElseGet(() -> licencePositionCorrectionService.newUpdatePositionCorrection(licenceCorrection, licencePosition));
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(positionCorrection));
    if (isOutOfRange(operations, index)) {
      return;
    }
    operations.remove(index);
    applyTransferEquity(positionCorrection, operations);
  }

  private List<TransferEquityOperation> transferEquityOperations(List<LicencePositionChangeType> changes) {
    return changes.stream()
        .filter(AddChange.class::isInstance)
        .map(AddChange.class::cast)
        .flatMap(change -> change.operations().stream())
        .filter(LicencePositionAddOperation.class::isInstance)
        .map(LicencePositionAddOperation.class::cast)
        .map(LicencePositionAddOperation::operation)
        .filter(TransferEquityOperation.class::isInstance)
        .map(TransferEquityOperation.class::cast)
        .toList();
  }

  private boolean isTransferEquityChange(LicencePositionChangeType change) {
    return !transferEquityOperations(List.of(change)).isEmpty();
  }

  private AddChange buildTransferEquityChange(
      List<TransferEquityOperation> operations,
      int changeOrder
  ) {
    var changeOperations = operations.stream()
        .map(
            operation -> (LicencePositionChangeOperation) LicencePositionChangeOperation.newLicencePositionAddOperation()
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

  private TransferEquityOperation toTransferEquityOperation(LicencePositionTransferEquityForm form) {
    return LicenceOperation.newTransferEquityOperation()
        .withTransferFrom(Integer.parseInt(form.getTransferFrom()))
        .withTransferTo(Integer.parseInt(form.getTransferTo()))
        .withEquity(form.getEquity().getAsBigDecimal().orElseThrow())
        .build();
  }

  private TransferEquityOperation withRetention(
      TransferEquityOperation operation,
      boolean retainsBeneficialInterest
  ) {
    return LicenceOperation.newTransferEquityOperation()
        .withTransferFrom(operation.transferFrom())
        .withTransferTo(operation.transferTo())
        .withEquity(operation.equity())
        .withRetainBeneficialInterest(retainsBeneficialInterest)
        .build();
  }

  private boolean isOutOfRange(List<TransferEquityOperation> operations, int index) {
    return index < 0 || index >= operations.size();
  }

  private void applyTransferEquity(
      LicencePositionCorrection licencePositionCorrection,
      List<TransferEquityOperation> operations
  ) {
    var payload = licencePositionCorrection.getPayload();

    var changes = payload.changes().stream()
        .filter(change -> !isTransferEquityChange(change))
        .collect(Collectors.toCollection(ArrayList::new));

    if (!CollectionUtils.isEmpty(operations)) {
      changes.add(buildTransferEquityChange(operations, licencePositionCorrectionService.nextChangeOrder(changes)));
    }

    licencePositionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionRepository.save(licencePositionCorrection);
  }
}