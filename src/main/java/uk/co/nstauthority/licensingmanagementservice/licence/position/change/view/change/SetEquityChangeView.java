package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record SetEquityChangeView(
    List<SetEquityRow> rows,
    @Nullable String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  @Override
  public String type() {
    return LicenceOperation.SET_EQUITY;
  }

  @Override
  public LicencePositionChangeView merge(LicencePositionChangeView other) {
    var otherView = (SetEquityChangeView) other;
    var combined = new ArrayList<>(rows);
    combined.addAll(otherView.rows());
    return new SetEquityChangeView(combined, changeType, urls.merge(otherView.urls()));
  }
}