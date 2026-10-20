package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "/licence-corrections/{correctionId}/position-correction/{licencePositionCorrectionId}/subarea-change/{blockFeatureId}"
)
@InvokingUserCanViewCorrection
@LicencePositionCorrectionBelongsToCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class SubareaChangeController {

  private static final String PAGE_TITLE = "Subarea change";
  private static final String SAVED_BANNER = "Subarea change saved";
  private static final String NO_CHANGE_ERROR = "You must make a change to the subareas before completing";

  private final SubareaChangeService subareaChangeService;
  private final FeatureService featureService;
  private final SubareaChangeValidator subareaChangeValidator;

  public SubareaChangeController(
      SubareaChangeService subareaChangeService,
      FeatureService featureService,
      SubareaChangeValidator subareaChangeValidator
  ) {
    this.subareaChangeService = subareaChangeService;
    this.featureService = featureService;
    this.subareaChangeValidator = subareaChangeValidator;
  }

  @GetMapping
  public ModelAndView renderSubareaChange(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @PathVariable UUID blockFeatureId
  ) {
    var commandJourneyId = subareaChangeService
        .getSubareaOperationOrThrow(licencePositionCorrection, blockFeatureId)
        .commandJourneyIdOrThrow();

    return getSubareaChangeModelAndView(correction, licencePositionCorrection, blockFeatureId, commandJourneyId);
  }

  @PostMapping
  ModelAndView complete(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @PathVariable UUID blockFeatureId,
      RedirectAttributes redirectAttributes
  ) {
    var commandJourneyId = subareaChangeService
        .getSubareaOperationOrThrow(licencePositionCorrection, blockFeatureId)
        .commandJourneyIdOrThrow();

    if (subareaChangeValidator.hasErrors(commandJourneyId)) {
      return getSubareaChangeModelAndView(correction, licencePositionCorrection, blockFeatureId, commandJourneyId)
          .addObject("errorSummaryItems", List.of(new ErrorSummaryItem(1, "subarea-change", NO_CHANGE_ERROR)))
          .addObject("mapErrorMessage", NO_CHANGE_ERROR);
    }

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    // TODO LMS2-269: redirect to review and submit when implemented
    return ReverseRouter.redirectToUrl(LicencePositionCorrectionRouteUtil.getPositionPageUrl(
        correction,
        licencePositionCorrection
    ));
  }

  private ModelAndView getSubareaChangeModelAndView(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId,
      UUID commandJourneyId
  ) {
    var coordinateSystem = featureService.getFeatureOrThrow(blockFeatureId).getCoordinateSystem();

    return new ModelAndView("lms/licence/correction/change/subarea/subareaChange")
        .addObject("commandJourneyId", commandJourneyId)
        .addObject("srsWkid", CoordinateSystemUtils.getWkid(coordinateSystem))
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("backLinkUrl", LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, licencePositionCorrection));
  }
}
