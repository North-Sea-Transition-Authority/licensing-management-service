package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record TransferEquityChangeView(
    List<TransferEquityChangeHoldingView> holdings,
    @Nullable String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  @Override
  public String type() {
    return LicenceOperation.TRANSFER_EQUITY;
  }

  @Override
  public LicencePositionChangeView merge(LicencePositionChangeView other) {
    var otherView = (TransferEquityChangeView) other;
    var combined = new ArrayList<>(holdings);
    combined.addAll(otherView.holdings());
    return new TransferEquityChangeView(combined, changeType, urls.merge(otherView.urls()));
  }
}