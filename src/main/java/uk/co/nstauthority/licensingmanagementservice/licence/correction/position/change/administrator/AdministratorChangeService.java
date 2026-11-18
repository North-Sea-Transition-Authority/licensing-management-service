package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
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

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final LicencePositionNameService licencePositionNameService;

  public AdministratorChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionChangeService licencePositionChangeService,
      ChronologicalPositionService chronologicalPositionService,
      LicencePositionNameService licencePositionNameService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
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
    var existingPositionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(licenceCorrection, licencePosition);

    LicencePositionCorrection positionCorrection;

    if (existingPositionCorrection.isPresent()) {
      positionCorrection = existingPositionCorrection.get();
      var payload = (UpdateLicencePositionPayload) positionCorrection.getPayload();

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

  private static AdministratorOperation administratorOperation(Integer administratorId) {
    return LicenceOperation.newAdministratorChange()
        .withOperator(administratorId)
        .build();
  }
}
