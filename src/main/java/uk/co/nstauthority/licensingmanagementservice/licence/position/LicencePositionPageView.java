package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.util.List;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionChangeView;

public record LicencePositionPageView(
    List<LicencePositionTimelineView> timelineViews,
    LicencePosition licencePosition,
    Map<String, LicencePositionChangeView> changeViewByType
) {

  public static LicencePositionPageView empty() {
    return new LicencePositionPageView(List.of(), null, Map.of());
  }

  public boolean hasPositions() {
    return !timelineViews.isEmpty();
  }
}
