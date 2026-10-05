package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

public class LicencePositionCorrectionRouteUtil {

  private LicencePositionCorrectionRouteUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static String getPositionPageUrl(UUID correctionId, LicencePositionCorrection positionCorrection) {
    return switch (positionCorrection.getChangeType()) {
      case ADD_POSITION -> ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderAddedPosition(correctionId, positionCorrection.getId(), null));
      case UPDATE_POSITION -> ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderLicencePosition(correctionId, positionCorrection.getTargetLicencePosition().getId(), null));
      case REMOVE_POSITION -> throw new IllegalStateException(
          "Licence position correction %s removes a position so cannot have changes made against it"
              .formatted(positionCorrection.getId()));
    };
  }
}
