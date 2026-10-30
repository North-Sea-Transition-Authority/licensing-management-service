package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStorageOperatorChangeUtil;

@Service
public class StorageOperatorChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  StorageOperatorChangeService(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
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
