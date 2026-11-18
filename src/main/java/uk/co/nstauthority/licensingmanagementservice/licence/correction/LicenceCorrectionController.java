package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.AddLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.FilteredCorrectionTimeline;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionFilterService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply.ReviewCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.update.UpdateCorrectionGeneralDetailsController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterForm;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterSession;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping("/licence-corrections")
@InvokingUserCanViewCorrection
@LogWorkAreaItemView(
    itemType = WorkAreaDataItemType.LICENCE_CORRECTION,
    pathVariable = "correctionId"
)
@SessionAttributes(LicenceCorrectionController.FILTER_SESSION_ATTRIBUTE)
public class LicenceCorrectionController {

  // Kept apart from the licence timeline's filters, so filtering a correction never filters the licence timeline.
  static final String FILTER_SESSION_ATTRIBUTE = "licenceCorrectionTimelineFilterSession";

  private final CorrectionPositionService correctionPositionService;
  private final LicencePositionCorrectionViewService licencePositionCorrectionViewService;
  private final LicencePositionCorrectionFilterService licencePositionCorrectionFilterService;
  private final LicenceService licenceService;
  private final CorrectionDetailsViewService correctionDetailsViewService;

  LicenceCorrectionController(
      CorrectionPositionService correctionPositionService,
      LicencePositionCorrectionViewService licencePositionCorrectionViewService,
      LicencePositionCorrectionFilterService licencePositionCorrectionFilterService,
      LicenceService licenceService,
      CorrectionDetailsViewService correctionDetailsViewService
  ) {
    this.correctionPositionService = correctionPositionService;
    this.licencePositionCorrectionViewService = licencePositionCorrectionViewService;
    this.licencePositionCorrectionFilterService = licencePositionCorrectionFilterService;
    this.licenceService = licenceService;
    this.correctionDetailsViewService = correctionDetailsViewService;
  }

  @GetMapping("/{correctionId}")
  public ModelAndView renderCorrection(
      LicenceCorrection licenceCorrection,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession
  ) {
    if (licenceCorrection.isComplete()) {
      return appliedCorrectionRedirect(licenceCorrection);
    }

    var filter = getFilter(licenceCorrection, filterSession);

    if (!filter.isEmpty()) {
      return switch (licencePositionCorrectionFilterService.getFilteredCorrectionTimeline(licenceCorrection, filter)) {
        case FilteredCorrectionTimeline.LatestMatchingPosition(String url) ->
            ReverseRouter.redirectToUrl(url);
        case FilteredCorrectionTimeline.NoMatchingPositions(LicencePositionPageView pageView) ->
            licencePositionsModelAndView(licenceCorrection, pageView, filter);
      };
    }

    var latestPositionId = latestPositionId(licenceCorrection);

    if (latestPositionId == null) {
      return licencePositionsModelAndView(licenceCorrection, LicencePositionPageView.empty(), filter);
    }

    return LicencePositionCorrectionRouteUtil.redirectToPositionPage(licenceCorrection, latestPositionId);
  }

  @GetMapping("/{correctionId}/positions/{licencePositionId}")
  public ModelAndView renderPosition(
      CorrectionPosition correctionPosition,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession
  ) {
    var licenceCorrection = correctionPosition.correction();

    if (licenceCorrection.isComplete()) {
      return appliedCorrectionRedirect(licenceCorrection);
    }

    var filter = getFilter(licenceCorrection, filterSession);
    var licencePositionPageView =
        licencePositionCorrectionViewService.getCorrectionPositionPageView(correctionPosition, filter);

    return licencePositionsModelAndView(licenceCorrection, licencePositionPageView, filter);
  }

  @PostMapping("/{correctionId}/filter")
  public ModelAndView filterCorrectionTimeline(
      LicenceCorrection licenceCorrection,
      @ModelAttribute("form") LicenceTimelineFilterForm form,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession
  ) {
    if (licenceCorrection.isComplete()) {
      return appliedCorrectionRedirect(licenceCorrection);
    }

    filterSession.update(
        filterKey(licenceCorrection),
        LicenceTimelineFilter.from(form, licencePositionCorrectionFilterService.getCorrectionFilterOptions(licenceCorrection))
    );

    return ReverseRouter.redirect(on(LicenceCorrectionController.class).renderCorrection(licenceCorrection, null));
  }

  @GetMapping("/{correctionId}/clear-filters")
  public ModelAndView clearCorrectionTimelineFilters(
      LicenceCorrection licenceCorrection,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession,
      SessionStatus sessionStatus
  ) {
    filterSession.clear(filterKey(licenceCorrection));

    if (filterSession.isEmpty()) {
      sessionStatus.setComplete();
    }

    return ReverseRouter.redirect(on(LicenceCorrectionController.class).renderCorrection(licenceCorrection, null));
  }

  @ModelAttribute(FILTER_SESSION_ATTRIBUTE)
  LicenceTimelineFilterSession getFilterSession() {
    return new LicenceTimelineFilterSession();
  }

  /**
   * Forgets the correction's timeline filter once the correction is applied or cancelled, as its timeline can no longer
   * be shown.
   */
  public static void clearFilter(HttpSession session, LicenceCorrection licenceCorrection) {
    if (!(session.getAttribute(FILTER_SESSION_ATTRIBUTE) instanceof LicenceTimelineFilterSession filterSession)) {
      return;
    }

    filterSession.clear(filterKey(licenceCorrection));

    if (filterSession.isEmpty()) {
      session.removeAttribute(FILTER_SESSION_ATTRIBUTE);
    } else {
      // Set again, as the session store only saves an attribute when it is set.
      session.setAttribute(FILTER_SESSION_ATTRIBUTE, filterSession);
    }
  }

  private static LicenceTimelineFilter getFilter(
      LicenceCorrection licenceCorrection,
      LicenceTimelineFilterSession filterSession
  ) {
    return filterSession.getFilter(filterKey(licenceCorrection));
  }

  private static String filterKey(LicenceCorrection licenceCorrection) {
    return licenceCorrection.getId().toString();
  }

  /**
   * The position the correction opens on: the latest position on its timeline, whether executed or staged by the
   * correction. Null when the timeline has no positions.
   */
  @Nullable
  private UUID latestPositionId(LicenceCorrection licenceCorrection) {
    var correctionPositions = correctionPositionService.getCorrectionPositions(licenceCorrection);
    return correctionPositions.isEmpty() ? null : correctionPositions.getLast().positionId();
  }

  private static ModelAndView appliedCorrectionRedirect(LicenceCorrection correction) {
    return ReverseRouter.redirect(on(ReviewCorrectionController.class)
        .renderReviewCorrection(correction));
  }

  private ModelAndView licencePositionsModelAndView(
      LicenceCorrection licenceCorrection,
      LicencePositionPageView licencePositionPageView,
      LicenceTimelineFilter filter
  ) {
    var licence =  licenceCorrection.getLicence();

    return new ModelAndView("lms/licence/correction/viewCorrection")
        .addObject("pageTitle", "%s - licence correction".formatted(licence.getLicenceReference()))
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("licencePositionPageView", licencePositionPageView)
        .addObject("correctionDetails", correctionDetailsViewService.getDetailsView(licenceCorrection))
        .addObject("addPositionUrl",
            ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
                .renderAddLicencePositionCorrection(licenceCorrection)))
        .addObject("updateGeneralDetailsUrl",
            ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
                .renderUpdateGeneralDetails(licenceCorrection)))
        .addObject("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
            .renderCancelCorrection(licenceCorrection)))
        .addObject("reviewCorrectionUrl", ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(licenceCorrection)))
        .addObject("form", LicenceTimelineFilterForm.from(filter))
        .addObject("filterUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
            .filterCorrectionTimeline(licenceCorrection, null, null)))
        .addObject("clearFilterUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
            .clearCorrectionTimelineFilters(licenceCorrection, null, null)));
  }

}