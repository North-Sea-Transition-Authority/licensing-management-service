package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import java.util.ArrayList;
import java.util.List;

public record TransferEquityChangeView(
    List<TransferEquityHoldingView> holdings,
    String changeType
) implements LicencePositionChangeView {

  @Override
  public LicencePositionChangeView merge(LicencePositionChangeView other) {
    var combined = new ArrayList<>(holdings);
    combined.addAll(((TransferEquityChangeView) other).holdings());
    return new TransferEquityChangeView(combined, changeType);
  }
}