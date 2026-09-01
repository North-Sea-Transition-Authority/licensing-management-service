package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionAdministratorChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil;

@Service
public class AdministratorChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionChangeService licencePositionChangeService;

  public AdministratorChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionChangeService licencePositionChangeService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Transactional
  public void addAdministratorChangeForAddedLicencePosition(
      LicencePositionCorrection licencePositionCorrection,
      Integer administratorId
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();

    var changes = LicencePositionAdministratorChangeUtil.upsertAddAdminChange(payload.changes(), administratorId);

    licencePositionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(licencePositionCorrection);
  }

  @Transactional
  public void addAdministratorChangeForExistingLicencePosition(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      Integer administratorId
  ) {
    var existingPositionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(licenceCorrection, licencePosition);

    LicencePositionCorrection positionCorrection;

    if (existingPositionCorrection.isPresent()) {
      positionCorrection = existingPositionCorrection.get();
      var payload = (UpdateLicencePositionPayload) positionCorrection.getPayload();

      var changes = LicencePositionAdministratorChangeUtil.upsertAddAdminChange(payload.changes(), administratorId);

      positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));

    } else {
      positionCorrection = new LicencePositionCorrection();
      var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of(
              AddChange.buildOperationsChange(List.of(administratorOperation(administratorId)), 1)))
          .build();

      positionCorrection.setLicenceCorrection(licenceCorrection);
      positionCorrection.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);
      positionCorrection.setTargetLicencePosition(licencePosition);
      positionCorrection.setPayload(payload);

    }

    licencePositionCorrectionService.save(positionCorrection);
  }

  @Transactional
  public void correctExistingAdministratorChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String originalChangeId,
      Integer administratorId
  ) {
    var existingPositionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(licenceCorrection, licencePosition);

    LicencePositionCorrection positionCorrection;

    if (existingPositionCorrection.isPresent()) {
      positionCorrection = existingPositionCorrection.get();
      var payload = (UpdateLicencePositionPayload) positionCorrection.getPayload();

      List<LicencePositionChangeType> changes;
      if (LicencePositionAdministratorChangeUtil.adminChangeExists(payload.changes())) {
        changes = LicencePositionAdministratorChangeUtil.replaceAdminChange(payload.changes(), administratorId);
      } else {
        var updatedChanges = new ArrayList<>(payload.changes());
        updatedChanges.add(
            UpdateChangeOperations.buildUpdateChange(originalChangeId, administratorOperation(administratorId))
        );
        changes = updatedChanges;
      }

      positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));

    } else {
      var originalChange = licencePositionChangeService.getByIdOrThrow(UUID.fromString(originalChangeId));

      if (LicencePositionAdministratorChangeUtil.adminIdNotChanged(originalChange, administratorId)) {
        return;
      }

      positionCorrection = new LicencePositionCorrection();
      var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of(UpdateChangeOperations
              .buildUpdateChange(originalChangeId, administratorOperation(administratorId)))
          )
          .build();

      positionCorrection.setLicenceCorrection(licenceCorrection);
      positionCorrection.setChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION);
      positionCorrection.setTargetLicencePosition(licencePosition);
      positionCorrection.setPayload(payload);

    }

    licencePositionCorrectionService.save(positionCorrection);
  }

  @Transactional
  public void removeExistingAdministratorChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String originalChangeId
  ) {
    licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .ifPresent(this::removeStagedAdministratorChange);

    licencePositionCorrectionService
        .stageRemovalOfExecutedChange(licenceCorrection, licencePosition, originalChangeId);
  }

  private void removeStagedAdministratorChange(LicencePositionCorrection positionCorrection) {
    var payload = positionCorrection.getPayload();
    var changes = LicencePositionAdministratorChangeUtil.removeAdminChange(payload.changes());

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }

  @Transactional
  public void undoAdministratorChange(
      LicenceCorrection licenceCorrection,
      String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);

    var payload = positionCorrection.getPayload();

    var changeToUndo = payload.changes().stream()
        .filter(not(LicencePositionChangeType::isUpdateChangeOrder))
        .filter(change -> changeId.equals(change.changeId()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No change with id %s found in position correction %s"
                .formatted(changeId, positionCorrection.getId()))
        );

    if (!isAdministratorChange(changeToUndo)) {
      throw new IllegalStateException(
          "Change with id %s is not an administrator change"
              .formatted(changeId));
    }

    var remainingChanges = LicencePositionChangeType.removeChangesById(payload.changes(), changeId);

    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.UPDATE_POSITION
        && remainingChanges.isEmpty()
        && LicencePositionChangeUtil.positionDateAndOrderUnchanged(positionCorrection)) {
      licencePositionCorrectionService.delete(positionCorrection);
      return;
    }

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, remainingChanges));
    licencePositionCorrectionService.save(positionCorrection);
  }

  public boolean hasPendingAdministratorChange(LicencePosition licencePosition, LicenceCorrection licenceCorrection) {
    return licencePositionCorrectionService
        .findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .map(positionCorrection ->
            LicencePositionAdministratorChangeUtil.adminChangeExists(positionCorrection.getPayload().changes()))
        .orElse(false);
  }

  private boolean isAdministratorChange(LicencePositionChangeType change) {
    if (change instanceof RemoveChange(String changeId)) {
      var liveChange = licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId));
      return LicencePositionAdministratorChangeUtil.containsAdminOperation(liveChange);
    }
    return LicencePositionAdministratorChangeUtil.containsAdminOperation(change);
  }

  private static AdministratorOperation administratorOperation(Integer administratorId) {
    return LicenceOperation.newAdministratorChange()
        .withOperator(administratorId)
        .build();
  }
}
