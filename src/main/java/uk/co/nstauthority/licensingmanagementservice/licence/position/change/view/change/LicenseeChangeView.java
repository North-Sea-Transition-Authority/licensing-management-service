package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

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
}
