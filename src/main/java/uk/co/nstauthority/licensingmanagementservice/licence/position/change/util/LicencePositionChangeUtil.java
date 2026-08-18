package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.List;
import java.util.Objects;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;

public final class LicencePositionChangeUtil {

  private LicencePositionChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static List<LicencePositionChangeType> removeChangeById(
      List<LicencePositionChangeType> changes,
      String changeId
  ) {
    return changes.stream()
        .filter(change -> !changeId.equals(change.changeId()))
        .toList();
  }

  public static boolean positionDateAndOrderUnchanged(LicencePositionCorrection positionCorrection) {
    var payload = (UpdateLicencePositionPayload) positionCorrection.getPayload();
    var licencePosition = positionCorrection.getTargetLicencePosition();
    var dateUnchanged = payload.effectiveDate() == null
        || payload.effectiveDate().equals(licencePosition.getPositionDate());
    var orderUnchanged = payload.effectiveDateOrder() == null
        || Objects.equals(payload.effectiveDateOrder(), licencePosition.getPositionDateOrder());
    return dateUnchanged && orderUnchanged;
  }

}