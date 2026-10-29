package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

public class LicencePositionCorrectionRouteUtil {

  private LicencePositionCorrectionRouteUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static String getPositionPageUrl(LicenceCorrection correction, LicencePositionCorrection positionCorrection) {
    return switch (positionCorrection.getChangeType()) {
      case ADD_POSITION -> ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction, positionCorrection, null));
      case UPDATE_POSITION -> ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition(), null));
      case REMOVE_POSITION -> throw new IllegalStateException(
          "Licence position correction %s removes a position so cannot have changes made against it"
              .formatted(positionCorrection.getId()));
    };
  }

  /**
   * The page for a position on the correction timeline: the added position's page when the correction adds the
   * position, or the executed position's page otherwise.
   *
   * @param addedPositionCorrection the correction adding the position, or null when the position is executed
   */
  public static String getPositionPageUrl(
      LicenceCorrection correction,
      UUID licencePositionId,
      @Nullable LicencePositionCorrection addedPositionCorrection
  ) {
    if (addedPositionCorrection != null) {
      return ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction, addedPositionCorrection, null));
    }
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, new LicencePosition(licencePositionId), null));
  }
}
