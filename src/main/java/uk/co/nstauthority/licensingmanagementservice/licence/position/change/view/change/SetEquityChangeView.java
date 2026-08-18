package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public record SetEquityChangeView(
    List<SetEquityRow> rows,
    @Nullable String changeType,
    @Nullable String updateUrl,
    @Nullable String undoUrl
) implements LicencePositionChangeView {

  @Override
  public LicencePositionChangeView merge(LicencePositionChangeView other) {
    var otherView = (SetEquityChangeView) other;
    var combined = new ArrayList<>(rows);
    combined.addAll(otherView.rows());
    return new SetEquityChangeView(
        combined,
        changeType,
        updateUrl != null ? updateUrl : otherView.updateUrl(),
        undoUrl != null ? undoUrl : otherView.undoUrl()
    );
  }
}