package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee.LicenseeChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.util.ListUtil;

public class LicencePositionLicenseeChangeUtil {

  private LicencePositionLicenseeChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static List<LicencePositionChangeType> upsertAddLicenseeChange(
      List<LicencePositionChangeType> changes, List<Integer> licenseesToAdd, List<Integer> licenseesToRemove
  ) {
    if (licenseeChangeExists(changes)) {
      return replaceLicenseeChange(changes, licenseesToAdd, licenseesToRemove);
    }
    var updatedChanges = new ArrayList<>(changes);
    updatedChanges.add(addLicenseeChange(licenseesToAdd, licenseesToRemove, updatedChanges.size() + 1));
    return updatedChanges;
  }

  public static LicenseeChangeForm populateLicenseeForm(LicenseeChangeContext licenseeChangeContext) {
    var form = new LicenseeChangeForm();
    if (licenseeChangeContext.currentJoiningLicenseeIds() != null) {
      form.setJoiningOrganisationIds(ListUtil.toStrings(licenseeChangeContext.currentJoiningLicenseeIds()));
    }
    if (licenseeChangeContext.currentWithdrawingLicenseeIds() != null) {
      form.setWithdrawingOrganisationIds(ListUtil.toStrings(licenseeChangeContext.currentWithdrawingLicenseeIds()));
    }
    return form;
  }

  public static boolean licenseeChangeExists(List<LicencePositionChangeType> changes) {
    return LicencePositionChangeOperationUtil.changeExists(changes, LicenseeOperation.class);
  }

  private static List<LicencePositionChangeType> replaceLicenseeChange(
      List<LicencePositionChangeType> changes, List<Integer> licenseesToAdd, List<Integer> licenseesToRemove
  ) {
    return LicencePositionChangeOperationUtil.replaceOperation(
        changes, LicenseeOperation.class, licenseeOperation(licenseesToAdd, licenseesToRemove));
  }

  private static LicenseeOperation licenseeOperation(List<Integer> licenseesToAdd, List<Integer> licenseesToRemove) {
    return LicenceOperation.newLicenseeOperation()
        .withLicenseesToAdd(licenseesToAdd)
        .withLicenseesToRemove(licenseesToRemove)
        .build();
  }

  private static AddChange addLicenseeChange(List<Integer> licenseesToAdd, List<Integer> licenseesToRemove, int changeOrder) {
    return AddChange.buildOperationsChange(List.of(licenseeOperation(licenseesToAdd, licenseesToRemove)), changeOrder);
  }
}
