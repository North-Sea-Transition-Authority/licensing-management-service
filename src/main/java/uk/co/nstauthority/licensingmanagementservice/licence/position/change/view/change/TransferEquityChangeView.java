package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public record TransferEquityChangeView(
    List<TransferEquityChangeHoldingView> holdings,
    @Nullable String changeType,
    @Nullable String updateUrl
) implements LicencePositionChangeView {

  @Override
  public LicencePositionChangeView merge(LicencePositionChangeView other) {
    var otherView = (TransferEquityChangeView) other;
    var combined = new ArrayList<>(holdings);
    combined.addAll(otherView.holdings());
    return new TransferEquityChangeView(combined, changeType, updateUrl != null ? updateUrl : otherView.updateUrl());
  }
}