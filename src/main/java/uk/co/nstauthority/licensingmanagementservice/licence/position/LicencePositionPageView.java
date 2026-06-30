package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.util.List;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;

public record LicencePositionPageView(
    List<LicencePositionTimelineView> timelineViews,
    LicencePosition licencePosition,
    Map<String, LicencePositionChangeView> changeViewByType,
    LicencePositionStateView stateView
) {

  public static LicencePositionPageView empty() {
    return new LicencePositionPageView(List.of(), null, Map.of(), null);
  }

  public boolean hasPositions() {
    return !timelineViews.isEmpty();
  }
}
