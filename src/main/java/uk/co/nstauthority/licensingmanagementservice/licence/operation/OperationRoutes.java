package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.ChangeUrlTarget;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public interface OperationRoutes {

  String correct(ChangeUrlTarget target);

  default String view(ChangeUrlTarget target) {
    return null;
  }

  static OperationRoutes none() {
    return target -> null;
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

  static String stagedPartialSurrenderTaskList(ChangeUrlTarget target) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(target.correction(), target.positionCorrection(), null));
  }

}
