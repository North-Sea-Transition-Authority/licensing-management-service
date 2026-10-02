package uk.co.nstauthority.licensingmanagementservice.licence.position;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterOptions;

/**
 * View model for a licence position page (read-only, correction, or added-position view).
 *
 * @param stateView the state view for the selected position
 * @param isAddedPosition true when the view represents a new position being added as part of a correction (which
 *                        has therefore not been executed), as opposed to an existing executed position or the read-only view
 * @param licenceType the type of the licence the position belongs to; null only for the empty view
 * @param filterOptions the change types and organisations the read-only timeline can be filtered by; none for every
 *                      other view
 * @param filterApplied true when the read-only timeline has been filtered
 */
public record LicencePositionPageView(
    List<LicencePositionTimelineView> timelineViews,
    String date,
    String regulatorReference,
    List<LicencePositionChangeView> orderedChangeViews,
    @Nullable LicencePositionStateView stateView,
    boolean canEdit,
    UUID selectedPositionId,
    boolean isAddedPosition,
    Actions actions,
    @Nullable LicenceType licenceType,
    List<ErrorSummaryItem> errorSummaryItems,
    LicenceTimelineFilterOptions filterOptions,
    boolean filterApplied
) {

  /**
   * Actions the current user can take from the position page.
   *
   * @param addChangeUrl URL of the generic "Add change" page (radio selection of change type); null when the action is
   *                     not offered. Populated for added correction positions ({@link #fromAddedPosition}), where
   *                     the change type is chosen before routing to the relevant journey.
   */
  public record Actions(@Nullable String addChangeUrl) {

    public static Actions none() {
      return new Actions(null);
    }
  }

  public static LicencePositionPageView empty() {
    return new LicencePositionPageView(
        List.of(),
        null,
        null,
        List.of(),
        null,
        false,
        null,
        false,
        Actions.none(),
        null,
        List.of(),
        LicenceTimelineFilterOptions.none(),
        false
    );
  }

  public static LicencePositionPageView readOnly(
      List<LicencePositionTimelineView> timelineViews,
      String date,
      String regulatorReference,
      List<LicencePositionChangeView> orderedChangeViews,
      LicencePositionStateView stateView,
      UUID selectedPositionId,
      LicenceType licenceType,
      LicenceTimelineFilterOptions filterOptions,
      boolean filterApplied
  ) {
    return new LicencePositionPageView(
        timelineViews,
        date,
        regulatorReference,
        orderedChangeViews,
        stateView,
        false,
        selectedPositionId,
        false,
        Actions.none(),
        licenceType,
        List.of(),
        filterOptions,
        filterApplied
    );
  }

  /**
   * The read-only view when the licence has positions but the filter leaves none of them.
   */
  public static LicencePositionPageView noMatchingPositions(
      LicenceType licenceType,
      LicenceTimelineFilterOptions filterOptions
  ) {
    return new LicencePositionPageView(
        List.of(),
        null,
        null,
        List.of(),
        null,
        false,
        null,
        false,
        Actions.none(),
        licenceType,
        List.of(),
        filterOptions,
        true
    );
  }

  public static LicencePositionPageView fromExecutedPosition(
      List<LicencePositionTimelineView> timelineViews,
      String date,
      String regulatorReference,
      List<LicencePositionChangeView> orderedChangeViews,
      LicencePositionStateView stateView,
      UUID selectedPositionId,
      Actions actions,
      LicenceType licenceType,
      List<ErrorSummaryItem> errorSummaryItems
  ) {
    return new LicencePositionPageView(
        timelineViews,
        date,
        regulatorReference,
        orderedChangeViews,
        stateView,
        true,
        selectedPositionId,
        false,
        actions,
        licenceType,
        errorSummaryItems,
        LicenceTimelineFilterOptions.none(),
        false
    );
  }

  public static LicencePositionPageView fromAddedPosition(
      List<LicencePositionTimelineView> timelineViews,
      String date,
      String regulatorReference,
      List<LicencePositionChangeView> orderedChangeViews,
      LicencePositionStateView stateView,
      UUID selectedPositionId,
      Actions actions,
      LicenceType licenceType,
      List<ErrorSummaryItem> errorSummaryItems
  ) {
    return new LicencePositionPageView(
        timelineViews,
        date,
        regulatorReference,
        orderedChangeViews,
        stateView,
        true,
        selectedPositionId,
        true,
        actions,
        licenceType,
        errorSummaryItems,
        LicenceTimelineFilterOptions.none(),
        false
    );
  }

  public boolean hasPositions() {
    return !timelineViews.isEmpty();
  }

  public boolean isCarbonStorage() {
    return licenceType == LicenceType.CARBON_STORAGE;
  }

}