package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

public class LicencePositionCorrectionRouteUtil {

  private LicencePositionCorrectionRouteUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  /**
   * The page for a position on the correction timeline, whether the position is executed or added in the correction.
   */
  public static String getPositionPageUrl(LicenceCorrection correction, UUID licencePositionId) {
    return ReverseRouter.route(
        on(LicenceCorrectionController.class).renderPosition(null, null),
        positionPageUriVariables(correction, licencePositionId)
    );
  }

  /**
   * The page for the position a position correction stages changes against: the position it adds, or the executed
   * position it updates.
   */
  public static String getPositionPageUrl(LicencePositionCorrection positionCorrection) {
    return getPositionPageUrl(positionCorrection.getLicenceCorrection(), getStagedPositionId(positionCorrection));
  }

  /**
   * Redirect to the page for the position a position correction stages changes against: the position it adds, or the
   * executed position it updates.
   */
  public static ModelAndView redirectToPositionPage(LicencePositionCorrection positionCorrection) {
    return redirectToPositionPage(positionCorrection.getLicenceCorrection(), getStagedPositionId(positionCorrection));
  }

  /**
   * The page for a position on the correction timeline, whether the position is executed or added in the correction.
   */
  public static ModelAndView redirectToPositionPage(LicenceCorrection correction, UUID licencePositionId) {
    return ReverseRouter.redirect(
        on(LicenceCorrectionController.class).renderPosition(null, null),
        positionPageUriVariables(correction, licencePositionId)
    );
  }

  /**
   * The old url for a position's page, which redirects to {@link #getPositionPageUrl(LicencePositionCorrection)}.
   * Removed once nothing calls it.
   */
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
   * The old url for a position's page, which redirects to {@link #getPositionPageUrl(LicenceCorrection, UUID)}.
   * Removed once nothing calls it.
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

  private static Map<String, Object> positionPageUriVariables(LicenceCorrection correction, UUID licencePositionId) {
    return Map.of(
        CorrectionPositionArgumentResolver.CORRECTION_ID, correction.getId(),
        CorrectionPositionArgumentResolver.LICENCE_POSITION_ID, licencePositionId
    );
  }

  private static UUID getStagedPositionId(LicencePositionCorrection positionCorrection) {
    return switch (positionCorrection.getChangeType()) {
      case ADD_POSITION -> positionCorrection.getPositionId();
      case UPDATE_POSITION -> positionCorrection.getTargetLicencePosition().getId();
      case REMOVE_POSITION -> throw new IllegalStateException(
          "Licence position correction %s removes a position so cannot have changes made against it"
              .formatted(positionCorrection.getId()));
    };
  }
}
