package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity.RemoveEquityChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.ChangeUrlTarget;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public interface OperationRoutes {

  String correct(ChangeUrlTarget target);

  String remove(ChangeUrlTarget target);

  String undo(ChangeUrlTarget target);

  static OperationRoutes none() {
    return new OperationRoutes() {
      @Override
      public String correct(ChangeUrlTarget target) {
        return null;
      }

      @Override
      public String remove(ChangeUrlTarget target) {
        return null;
      }

      @Override
      public String undo(ChangeUrlTarget target) {
        return null;
      }
    };
  }

  /**
   * An equity change can only be corrected once this correction has staged it, by adding or updating it.
   */
  static boolean isStagedEquityChange(ChangeUrlTarget target) {
    return switch (target.state()) {
      case ADDED, OPERATIONS_UPDATED -> true;
      case LIVE, ORDER_UPDATED, REMOVED -> false;
    };
  }

  static String removeEquityChange(ChangeUrlTarget target) {
    return ReverseRouter.route(on(RemoveEquityChangeController.class)
        .renderRemoveExecutedEquityChange(target.correction(), target.licencePosition(), target.changeEntity()));
  }

  static String undoEquityChange(ChangeUrlTarget target) {
    return ReverseRouter.route(on(RemoveEquityChangeController.class)
        .renderUndoEquityChange(target.correction(), target.changeId()));
  }

  static String stagedPartialSurrenderTaskList(ChangeUrlTarget target) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(target.correction(), target.positionCorrection(), null));
  }

}
