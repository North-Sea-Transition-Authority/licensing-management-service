package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Map;
import java.util.UUID;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

public class LicencePositionStagedPayloadRouteUtil {

  private LicencePositionStagedPayloadRouteUtil() {
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
   * The page for a position on the correction timeline, whether the position is executed or added in the correction.
   */
  public static String getPositionPageUrl(CorrectionPosition correctionPosition) {
    return ReverseRouter.route(on(LicenceCorrectionController.class).renderPosition(correctionPosition, null));
  }

  /**
   * The page for the position a position staged payload stages changes against: the position it adds, or the executed
   * position it updates.
   */
  public static String getPositionPageUrl(LicencePositionStagedPayload positionStagedPayload) {
    return getPositionPageUrl(positionStagedPayload.getLicenceCorrection(), getStagedPositionId(positionStagedPayload));
  }

  /**
   * Redirect to the page for the position a position staged payload stages changes against: the position it adds, or the
   * executed position it updates.
   */
  public static ModelAndView redirectToPositionPage(LicencePositionStagedPayload positionStagedPayload) {
    return redirectToPositionPage(positionStagedPayload.getLicenceCorrection(), getStagedPositionId(positionStagedPayload));
  }

  /**
   * Redirect to the page for a position on the correction timeline, whether the position is executed or added in the
   * correction.
   */
  public static ModelAndView redirectToPositionPage(CorrectionPosition correctionPosition) {
    return ReverseRouter.redirect(on(LicenceCorrectionController.class).renderPosition(correctionPosition, null));
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

  private static Map<String, Object> positionPageUriVariables(LicenceCorrection correction, UUID licencePositionId) {
    return Map.of(
        CorrectionPositionArgumentResolver.CORRECTION_ID, correction.getId(),
        CorrectionPositionArgumentResolver.LICENCE_POSITION_ID, licencePositionId
    );
  }

  private static UUID getStagedPositionId(LicencePositionStagedPayload positionStagedPayload) {
    return switch (positionStagedPayload.getChangeType()) {
      case ADD_POSITION -> positionStagedPayload.getPositionId();
      case UPDATE_POSITION -> positionStagedPayload.getTargetLicencePosition().getId();
      case REMOVE_POSITION -> throw new IllegalStateException(
          "Licence position staged payload %s removes a position so cannot have changes made against it"
              .formatted(positionStagedPayload.getId()));
    };
  }
}
