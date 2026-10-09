package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition.CorrectPositionChangeTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

/**
 * Decides which actions a change offers on the position it is viewed on, and builds their urls.
 */
final class ChangeViewUrlsFactory {

  private ChangeViewUrlsFactory() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  /**
   * The urls for one operation of a change, or none when the position is not being viewed within a correction.
   *
   * @param canReorder whether more than one change on the position can be reordered
   */
  static ChangeViewUrls build(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change,
      UUID positionId,
      boolean canReorder,
      VisibleLicenceOperation operation
  ) {
    if (urlContext == null) {
      return ChangeViewUrls.none();
    }

    var target = new ChangeUrlTarget(urlContext, change);
    var routes = operation.getOperationUrls();

    return new ChangeViewUrls(
        routes.correct(target),
        canRemove(target) ? routes.remove(target) : null,
        canUndo(target) ? routes.undo(target) : null,
        canReorder && change.canBeReordered() ? correctChangeOrderUrl(target, positionId) : null,
        change.canBeReordered() ? correctPositionUrl(target, positionId) : null
    );
  }

  /**
   * Only a live change left untouched by this correction can be removed, and only from the position that holds it.
   */
  private static boolean canRemove(ChangeUrlTarget target) {
    return !target.addedPosition() && target.state() == ChangeUrlTarget.State.LIVE;
  }

  /**
   * Only a change staged by this correction can be undone, whether it was added, corrected or removed.
   */
  private static boolean canUndo(ChangeUrlTarget target) {
    return target.state() != ChangeUrlTarget.State.LIVE && !target.change().movedAway();
  }

  private static String correctChangeOrderUrl(ChangeUrlTarget target, UUID positionId) {
    return ReverseRouter.route(on(CorrectChangeOrderController.class)
        .renderCorrectChangeOrder(target.correction(), positionId, UUID.fromString(target.changeId())));
  }

  private static String correctPositionUrl(ChangeUrlTarget target, UUID positionId) {
    return ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
        .renderMoveChangeTypePosition(target.correction().getId(), positionId, UUID.fromString(target.changeId()), null));
  }
}
