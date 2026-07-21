package uk.co.nstauthority.licensingmanagementservice.licence.position;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;

/**
 * View model for a licence position page (read-only, correction, or added-position view).
 *
 * @param stateView the state view for the selected live position; null when there is no selected live position
 * @param isAddedPosition true when the view represents a new position being added as part of a correction (which
 *                        has therefore not been executed), as opposed to an existing executed position or the read-only view
 */
public record LicencePositionPageView(
    List<LicencePositionTimelineView> timelineViews,
    String date,
    String regulatorReference,
    Map<String, LicencePositionChangeView> changeViewByType,
    @Nullable LicencePositionStateView stateView,
    boolean canEdit,
    UUID selectedPositionId,
    boolean isAddedPosition,
    Actions actions
) {

  /**
   * Actions the current user can take from the position page.
   *
   * @param addAdministratorChangeUrl URL of the change-licence-administrator journey; null when the action is not
   *                                  offered. It is only populated for the correction
   *                                  views ({@link #fromExecutedPosition} and {@link #fromNonExecutedPosition}), where
   *                                  a regulator is editing a correction and may change the administrator.
   */
  //TODO LMS2-86: We can use ActionItemView when more actions are added instead
  public record Actions(@Nullable String addAdministratorChangeUrl) {

    public static Actions none() {
      return new Actions(null);
    }
  }

  public static LicencePositionPageView empty() {
    return new LicencePositionPageView(List.of(), null, null, Map.of(), null, false, null, false, Actions.none());
  }

  public static LicencePositionPageView readOnly(
      List<LicencePositionTimelineView> timelineViews,
      String date,
      String regulatorReference,
      Map<String, LicencePositionChangeView> changeViewByType,
      @Nullable LicencePositionStateView stateView,
      UUID selectedPositionId
  ) {
    return new LicencePositionPageView(
        timelineViews, date, regulatorReference, changeViewByType, stateView, false, selectedPositionId, false,
        Actions.none());
  }

  public static LicencePositionPageView fromExecutedPosition(
      List<LicencePositionTimelineView> timelineViews,
      String date,
      String regulatorReference,
      Map<String, LicencePositionChangeView> changeViewByType,
      @Nullable LicencePositionStateView stateView,
      UUID selectedPositionId,
      Actions actions
  ) {
    return new LicencePositionPageView(
        timelineViews, date, regulatorReference, changeViewByType, stateView, true, selectedPositionId, false, actions);
  }

  public static LicencePositionPageView fromNonExecutedPosition(
      List<LicencePositionTimelineView> timelineViews,
      String date,
      String regulatorReference,
      UUID selectedPositionId,
      Actions actions
  ) {
    return new LicencePositionPageView(
        timelineViews, date, regulatorReference, Map.of(), null, true, selectedPositionId, true, actions);
  }

  public boolean hasPositions() {
    return !timelineViews.isEmpty();
  }
}