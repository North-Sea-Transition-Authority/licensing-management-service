package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

public class LicencePositionCorrectionRouteUtil {

  private LicencePositionCorrectionRouteUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static String getPositionPageUrl(LicenceCorrection correction, LicencePositionCorrection positionCorrection) {
    return switch (positionCorrection.getChangeType()) {
      case ADD_POSITION -> ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction, positionCorrection));
      case UPDATE_POSITION -> ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition()));
      case REMOVE_POSITION -> throw new IllegalStateException(
          "Licence position correction %s removes a position so cannot have changes made against it"
              .formatted(positionCorrection.getId()));
    };
  }
}
