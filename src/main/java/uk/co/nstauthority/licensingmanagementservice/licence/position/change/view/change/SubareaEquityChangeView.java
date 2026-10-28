package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record SubareaEquityChangeView(
    String subareaLabel,
    List<SetEquityRow> rows,
    @Nullable String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  @Override
  public String type() {
    return LicenceOperation.SUBAREA_SET_EQUITY;
  }

  @Override
  public LicencePositionChangeView merge(LicencePositionChangeView other) {
    var otherView = (SubareaEquityChangeView) other;
    var combined = new ArrayList<>(rows);
    combined.addAll(otherView.rows());
    return new SubareaEquityChangeView(subareaLabel, combined, changeType, urls.merge(otherView.urls()));
  }
}