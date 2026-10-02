package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionLicenseeChangeUtil;

@Service
public class LicenseeChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  LicenseeChangeService(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @Transactional
  public void addLicenseeChangeForAddedLicencePosition(
      LicencePositionCorrection licencePositionCorrection,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    stageLicenseeChange(licencePositionCorrection, joiningLicenseeIds, withdrawingLicenseeIds);
  }

  @Transactional
  public void addLicenseeChangeForExistingLicencePosition(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    stageLicenseeChange(positionCorrection, joiningLicenseeIds, withdrawingLicenseeIds);
  }

  private void stageLicenseeChange(
      LicencePositionCorrection positionCorrection,
      List<Integer> joiningLicenseeIds,
      List<Integer> withdrawingLicenseeIds
  ) {
    var payload = positionCorrection.getPayload();

    var changes = LicencePositionLicenseeChangeUtil.upsertAddLicenseeChange(
        payload.changes(),
        joiningLicenseeIds,
        withdrawingLicenseeIds
    );

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(positionCorrection);
  }
}
