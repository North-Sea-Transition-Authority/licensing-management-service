package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStorageOperatorChangeUtil;

@Service
public class StorageOperatorChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionViewService licencePositionViewService;

  StorageOperatorChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionViewService licencePositionViewService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionViewService = licencePositionViewService;
  }

  @Transactional
  public void addStorageOperatorChangeForAddedLicencePosition(
      LicencePositionCorrection licencePositionCorrection,
      Integer operatorId
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();

    var changes = LicencePositionStorageOperatorChangeUtil.upsertAddStorageOperatorChange(payload.changes(), operatorId);

    licencePositionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(licencePositionCorrection);
  }

  @Transactional
  public void addStorageOperatorChangeForExistingLicencePosition(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      Integer operatorId
  ) {
    var existingPositionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(licenceCorrection, licencePosition);

    LicencePositionCorrection positionCorrection;

    if (existingPositionCorrection.isPresent()) {
      positionCorrection = existingPositionCorrection.get();
      var payload = (UpdateLicencePositionPayload) positionCorrection.getPayload();

      var changes = LicencePositionStorageOperatorChangeUtil
          .upsertAddStorageOperatorChange(payload.changes(), operatorId);

      positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    } else {
      positionCorrection = new LicencePositionCorrection();
      var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of(AddChange.buildOperationsChange(
                  List.of(LicencePositionStorageOperatorChangeUtil.storageOperatorOperation(operatorId)),
                  1)))
          .build();

      positionCorrection.setLicenceCorrection(licenceCorrection);
      positionCorrection.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);
      positionCorrection.setTargetLicencePosition(licencePosition);
      positionCorrection.setPayload(payload);
    }
    licencePositionCorrectionService.save(positionCorrection);
  }

  @Transactional
  public void correctExistingStorageOperatorChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      Integer storageOperatorId,
      String originalChangeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    var payload = positionCorrection.getPayload();

    var changes = LicencePositionChangeOperationUtil.upsertUpdateChange(
        payload.changes(),
        StorageOperatorOperation.class,
        originalChangeId,
        LicencePositionStorageOperatorChangeUtil.storageOperatorOperation(storageOperatorId)
    );

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }

  @Transactional
  public void removeExistingStorageOperatorChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String originalChangeId
  ) {
    licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .ifPresent(this::removeStagedStorageOperatorChange);

    licencePositionCorrectionService
        .stageRemovalOfExecutedChange(licenceCorrection, licencePosition, originalChangeId);
  }

  @Transactional
  public void undoStorageOperatorChange(
      LicenceCorrection licenceCorrection,
      String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);

    var changeToUndo = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId);

    var isStorageOperatorChange = licencePositionCorrectionService.resolveStagedChangeOperations(changeToUndo).stream()
        .anyMatch(StorageOperatorOperation.class::isInstance);

    if (!isStorageOperatorChange) {
      throw new IllegalStateException(
          "Change with id %s is not an storage operator change"
              .formatted(changeId));
    }

    licencePositionCorrectionService.dropStagedChange(positionCorrection, changeId);
  }

  public String getJoiningOperatorForReinstating(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrectionToUndo
  ) {
    var licencePositionId = positionCorrectionToUndo.getPositionId();

    var remainingPositionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection)
        .stream()
        .filter(correction -> !correction.getId().equals(positionCorrectionToUndo.getId()))
        .toList();

    var targetPositions = licencePositionViewService.getCorrectedChronologicalPositions(
        licenceCorrection,
        remainingPositionCorrections,
        Set.of(licencePositionId)
    );

    var uncorrectedChangeContext = licencePositionViewService
        .getStorageOperatorChangeContext(licenceCorrection, licencePositionId, targetPositions);

    return uncorrectedChangeContext.currentOperatorName();
  }

  private void removeStagedStorageOperatorChange(LicencePositionCorrection positionCorrection) {
    var payload = positionCorrection.getPayload();
    var changes = LicencePositionStorageOperatorChangeUtil.removeStorageOperatorChange(payload.changes());

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }

  public StorageOperatorChangeForm populateAddedStorageOperatorForm(
      StorageOperatorChangeContext changeContext,
      LicencePositionCorrection positionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
    var form = new StorageOperatorChangeForm();
    if (changeContext.currentOperatorId() != null
        && LicencePositionStorageOperatorChangeUtil.storageOperatorChangeExists(payload.changes())
    ) {
      form.getOperatorId().setInputValue(String.valueOf(changeContext.currentOperatorId()));
    }
    return form;
  }

  public StorageOperatorChangeForm populateExistingStorageOperatorForm(
      StorageOperatorChangeContext changeContext,
      LicenceCorrection correction,
      LicencePosition position
  ) {
    var form = new StorageOperatorChangeForm();
    if (changeContext.currentOperatorId() != null
        && hasPendingStorageOperatorChange(position, correction)
    ) {
      form.getOperatorId().setInputValue(String.valueOf(changeContext.currentOperatorId()));
    }
    return form;
  }

  public StorageOperatorChangeForm populateCorrectingStorageOperatorForm(
      StorageOperatorChangeContext changeContext
  ) {
    var form = new StorageOperatorChangeForm();
    if (changeContext.currentOperatorId() != null) {
      form.getOperatorId().setInputValue(String.valueOf(changeContext.currentOperatorId()));
    }
    return form;
  }

  public boolean hasPendingStorageOperatorChange(LicencePosition licencePosition, LicenceCorrection licenceCorrection) {
    return licencePositionCorrectionService
        .findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .map(positionCorrection ->
            LicencePositionStorageOperatorChangeUtil.storageOperatorChangeExists(positionCorrection.getPayload().changes()))
        .orElse(false);
  }
}
