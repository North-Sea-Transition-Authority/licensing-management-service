package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record LicenseeChangeView(
    List<String> withdrawingLicensees,
    List<String> joiningLicensees,
    String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  @Override
  public String type() {
    return LicenceOperation.LICENSEE;
  }

  @Override
  public LicencePositionChangeView merge(LicencePositionChangeView other) {
    var otherView = (LicenseeChangeView) other;
    var combinedWithdrawingLicensees = new ArrayList<>(withdrawingLicensees);
    combinedWithdrawingLicensees.addAll(otherView.withdrawingLicensees());

    var combinedJoiningLicensees = new ArrayList<>(joiningLicensees);
    combinedJoiningLicensees.addAll(otherView.joiningLicensees);
    return new LicenseeChangeView(combinedWithdrawingLicensees,
        combinedJoiningLicensees,
        changeType,
        urls.merge(otherView.urls()));
  }
}
