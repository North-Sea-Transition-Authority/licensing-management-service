package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public record SetEquityChangeView(
    List<SetEquityRow> rows,
    @Nullable String changeType
) implements LicencePositionChangeView {

  @Override
  public LicencePositionChangeView merge(LicencePositionChangeView other) {
    var combined = new ArrayList<>(rows);
    combined.addAll(((SetEquityChangeView) other).rows());
    return new SetEquityChangeView(combined, changeType);
  }
}