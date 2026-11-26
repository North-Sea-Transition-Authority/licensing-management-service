package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class TransferEquityCorrectionService {

  private final LicencePositionStagedPayloadRepository licencePositionStagedPayloadRepository;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final ChronologicalPositionService chronologicalPositionService;

  TransferEquityCorrectionService(
      LicencePositionStagedPayloadRepository licencePositionStagedPayloadRepository,
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      OrganisationUnitQueryService organisationUnitQueryService,
      ChronologicalPositionService chronologicalPositionService
  ) {
    this.licencePositionStagedPayloadRepository = licencePositionStagedPayloadRepository;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.chronologicalPositionService = chronologicalPositionService;
  }

  public Map<Integer, BigDecimal> getEquityHoldingsForCorrection(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId
  ) {
    var chronologicalPositions = chronologicalPositionService
        .getCorrectedChronologicalPositions(licenceCorrection, licencePositionId);
    return resolveEquityHoldings(licencePositionId, chronologicalPositions);
  }

  private Map<Integer, BigDecimal> resolveEquityHoldings(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    var holdings = new HashMap<Integer, BigDecimal>();

    for (var chronologicalPosition : chronologicalPositions) {
      chronologicalPosition.changes().stream()
          .filter(change -> !LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType()))
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
      if (holdings.get(transferFrom).signum() <= 0 && !Boolean.TRUE.equals(retainBeneficialInterest)) {
        holdings.remove(transferFrom);
      }
    }
  }

  public List<TransferEquityOperation> getCommittedTransferEquityOperations(
      LicencePositionStagedPayload licencePositionStagedPayload
  ) {
    return transferEquityOperations(licencePositionStagedPayload.getPayload().changes());
  }

  @Transactional
  public void addTransferEquity(
      LicencePositionStagedPayload licencePositionStagedPayload,
      LicencePositionTransferEquityForm form
  ) {
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(licencePositionStagedPayload));
    operations.add(toTransferEquityOperation(form));
    applyTransferEquity(licencePositionStagedPayload, operations);
  }

  @Transactional
  public void setTransferEquityRetention(
      LicencePositionStagedPayload licencePositionStagedPayload,
      int index,
      boolean retainsBeneficialInterest
  ) {
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(licencePositionStagedPayload));
    if (isOutOfRange(operations, index)) {
      return;
    }
    operations.set(index, withRetention(operations.get(index), retainsBeneficialInterest));
    applyTransferEquity(licencePositionStagedPayload, operations);
  }

  @Transactional
  public void removeTransferEquity(LicencePositionStagedPayload licencePositionStagedPayload, int index) {
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(licencePositionStagedPayload));
    if (isOutOfRange(operations, index)) {
      return;
    }
    operations.remove(index);
    applyTransferEquity(licencePositionStagedPayload, operations);
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

  public Map<String, String> getLicenseeOptions(Map<Integer, BigDecimal> equityHoldings) {
    return getTransferToOptions(equityHoldings.entrySet().stream()
        .filter(holding -> holding.getValue().compareTo(BigDecimal.ZERO) > 0)
        .collect(StreamUtil.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  public Map<String, String> getTransferToOptions(Map<Integer, BigDecimal> equityHoldings) {
    var holdings = equityHoldings.entrySet();

    if (holdings.isEmpty()) {
      return Map.of();
    }

    var organisationNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(
        holdings.stream().map(Map.Entry::getKey).toList()
    );

    return holdings.stream()
        .map(holding -> Map.entry(
            String.valueOf(holding.getKey()),
            "%s (%s%%)".formatted(
                organisationNames.getOrDefault(holding.getKey(), NOT_AVAILABLE),
                holding.getValue().stripTrailingZeros().toPlainString())
        ))
        .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
        .collect(StreamUtil.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<Integer, BigDecimal> getEquityHoldings(CorrectionPosition correctionPosition) {
    return getEquityHoldingsForCorrection(correctionPosition.correction(), correctionPosition.positionId());
  }

  public List<TransferEquityOperation> getCommittedTransferEquityOperations(CorrectionPosition correctionPosition) {
    return switch (correctionPosition) {
      case CorrectionPosition.Executed executed ->
          getCommittedTransferEquityOperationsForExecutedPosition(executed.correction(), executed.licencePosition());
      case CorrectionPosition.Staged staged -> getCommittedTransferEquityOperations(staged.licencePositionStagedPayload());
    };
  }

  @Transactional
  public void addTransferEquity(CorrectionPosition correctionPosition, LicencePositionTransferEquityForm form) {
    switch (correctionPosition) {
      case CorrectionPosition.Executed(LicenceCorrection correction, LicencePosition licencePosition) ->
          addTransferEquityForExecutedPosition(correction, licencePosition, form);
      case CorrectionPosition.Staged(LicencePositionStagedPayload licencePositionStagedPayload) ->
          addTransferEquity(licencePositionStagedPayload, form);
    }
  }

  @Transactional
  public void setTransferEquityRetention(CorrectionPosition correctionPosition, int index, boolean retainsBeneficialInterest) {
    switch (correctionPosition) {
      case CorrectionPosition.Executed(LicenceCorrection correction, LicencePosition licencePosition) ->
          setTransferEquityRetentionForExecutedPosition(correction, licencePosition, index, retainsBeneficialInterest);
      case CorrectionPosition.Staged(LicencePositionStagedPayload licencePositionStagedPayload) ->
          setTransferEquityRetention(licencePositionStagedPayload, index, retainsBeneficialInterest);
    }
  }

  @Transactional
  public void removeTransferEquity(CorrectionPosition correctionPosition, int index) {
    switch (correctionPosition) {
      case CorrectionPosition.Executed executed ->
          removeTransferEquityForExecutedPosition(executed.correction(), executed.licencePosition(), index);
      case CorrectionPosition.Staged staged -> removeTransferEquity(staged.licencePositionStagedPayload(), index);
    }
  }

  private List<TransferEquityOperation> getCommittedTransferEquityOperationsForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return licencePositionStagedPayloadService.findUpdatePositionStagedPayload(licenceCorrection, licencePosition)
        .map(this::getCommittedTransferEquityOperations)
        .orElseGet(List::of);
  }

  private void addTransferEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionTransferEquityForm form
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .findUpdatePositionStagedPayload(licenceCorrection, licencePosition)
        .orElseGet(() -> licencePositionStagedPayloadService.newUpdatePositionStagedPayload(licenceCorrection, licencePosition));
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(positionStagedPayload));
    operations.add(toTransferEquityOperation(form));
    applyTransferEquity(positionStagedPayload, operations);
  }

  private void setTransferEquityRetentionForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      int index,
      boolean retainsBeneficialInterest
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .findUpdatePositionStagedPayload(licenceCorrection, licencePosition)
        .orElseGet(() -> licencePositionStagedPayloadService.newUpdatePositionStagedPayload(licenceCorrection, licencePosition));
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(positionStagedPayload));
    if (isOutOfRange(operations, index)) {
      return;
    }
    operations.set(index, withRetention(operations.get(index), retainsBeneficialInterest));
    applyTransferEquity(positionStagedPayload, operations);
  }

  private void removeTransferEquityForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      int index
  ) {
    var positionStagedPayload = licencePositionStagedPayloadService
        .findUpdatePositionStagedPayload(licenceCorrection, licencePosition)
        .orElseGet(() -> licencePositionStagedPayloadService.newUpdatePositionStagedPayload(licenceCorrection, licencePosition));
    var operations = new ArrayList<>(getCommittedTransferEquityOperations(positionStagedPayload));
    if (isOutOfRange(operations, index)) {
      return;
    }
    operations.remove(index);
    applyTransferEquity(positionStagedPayload, operations);
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
      LicencePositionStagedPayload licencePositionStagedPayload,
      List<TransferEquityOperation> operations
  ) {
    var payload = licencePositionStagedPayload.getPayload();

    var changes = payload.changes().stream()
        .filter(change -> !isTransferEquityChange(change))
        .collect(Collectors.toCollection(ArrayList::new));

    if (!CollectionUtils.isEmpty(operations)) {
      changes.add(buildTransferEquityChange(operations, licencePositionStagedPayloadService.nextChangeOrder(changes)));
    }

    licencePositionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionStagedPayloadRepository.save(licencePositionStagedPayload);
  }
}
