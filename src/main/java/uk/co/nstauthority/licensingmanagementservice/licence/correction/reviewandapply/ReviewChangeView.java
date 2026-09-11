package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;

public record ReviewChangeView(
    LicencePositionChangeView change,
    @Nullable LicencePositionChangeView previousChange,
    @Nullable Integer changeOrder
) {

  private static final Comparator<ReviewChangeView> BY_CHANGE_ORDER =
      Comparator.comparing(ReviewChangeView::changeOrder, Comparator.nullsLast(Comparator.naturalOrder()));

  @Nullable
  public CorrectionMarker marker() {
    return change.marker();
  }

  static List<ReviewChangeView> forPosition(
      ChronologicalPosition position,
      ReviewPositionContext correctedContext,
      ChangeEdits changeEdits
  ) {
    var changeOrdersByChangeId = position.changes().stream()
        .filter(change -> change.changeOrder() != null)
        .collect(Collectors.toMap(PositionChange::changeId, PositionChange::changeOrder));
    var changes = new ArrayList<ReviewChangeView>();

    LicencePositionChangeViewResolver.getChangeViewsByChangeId(
        position.id(),
        correctedContext.positions(),
        correctedContext.resolvedStates(),
        correctedContext.organisationNames(),
        correctedContext.featureNames(),
        null
    ).forEach((changeId, changeViews) -> changeViews.forEach(changeView -> changes.add(
        changeEdits.reviewChange(changeView, changeId, changeOrdersByChangeId.get(changeId)))));

    return changes.stream()
        .sorted(BY_CHANGE_ORDER)
        .toList();
  }
}
