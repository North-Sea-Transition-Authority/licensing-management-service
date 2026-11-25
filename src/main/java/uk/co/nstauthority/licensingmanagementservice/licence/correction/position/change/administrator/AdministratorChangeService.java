package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionAdministratorChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;

@Service
public class AdministratorChangeService {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionNameService licencePositionNameService;

  public AdministratorChangeService(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      LicencePositionChangeService licencePositionChangeService,
      ChronologicalPositionService chronologicalPositionService,
      LicencePositionNameService licencePositionNameService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.chronologicalPositionService = chronologicalPositionService;
    this.licencePositionNameService = licencePositionNameService;
  }

  @Transactional
  public void correctExistingAdministratorChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String originalChangeId,
      Integer administratorId
  ) {
    var existingPositionStagedPayload = licencePositionStagedPayloadService
        .findUpdatePositionStagedPayload(licenceCorrection, licencePosition);

    LicencePositionStagedPayload positionStagedPayload;

    if (existingPositionStagedPayload.isPresent()) {
      positionStagedPayload = existingPositionStagedPayload.get();
      var payload = (UpdateLicencePositionPayload) positionStagedPayload.getPayload();

      List<LicencePositionChangeType> changes;
      if (LicencePositionAdministratorChangeUtil.adminChangeExists(payload.changes())) {
        changes = LicencePositionChangeOperationUtil.replaceOperation(
            payload.changes(),
            AdministratorOperation.class,
            administratorOperation(administratorId)
        );
      } else {
        var updatedChanges = new ArrayList<>(payload.changes());
        updatedChanges.add(
            UpdateChangeOperations.buildUpdateChange(originalChangeId, administratorOperation(administratorId))
        );
        changes = updatedChanges;
      }

      positionStagedPayload.setPayload(LicencePositionPayload.withChanges(payload, changes));

    } else {
      var originalChange = licencePositionChangeService.getByIdOrThrow(UUID.fromString(originalChangeId));

      if (LicencePositionAdministratorChangeUtil.adminIdNotChanged(originalChange, administratorId)) {
        return;
      }

      positionStagedPayload = new LicencePositionStagedPayload();
      var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
          .withCorrectionReference(licenceCorrection.getCorrectionReference())
          .withChanges(List.of(UpdateChangeOperations
              .buildUpdateChange(originalChangeId, administratorOperation(administratorId)))
          )
          .build();

      positionStagedPayload.setLicenceCorrection(licenceCorrection);
      positionStagedPayload.setChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
      positionStagedPayload.setTargetLicencePosition(licencePosition);
      positionStagedPayload.setPayload(payload);

    }

    licencePositionStagedPayloadService.save(positionStagedPayload);
  }

  private static AdministratorOperation administratorOperation(Integer administratorId) {
    return LicenceOperation.newAdministratorChange()
        .withOperator(administratorId)
        .build();
  }
}
