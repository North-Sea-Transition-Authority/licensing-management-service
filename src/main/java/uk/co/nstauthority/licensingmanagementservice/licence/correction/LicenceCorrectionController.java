package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.servlet.http.HttpSession;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.AddLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply.ReviewCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.update.UpdateCorrectionGeneralDetailsController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.FilteredCorrectionTimeline;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
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

  private final LicencePositionService licencePositionService;
  private final LicencePositionViewService licencePositionViewService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicenceService licenceService;
  private final CorrectionDetailsViewService correctionDetailsViewService;

  public LicenceCorrectionController(
      LicencePositionService licencePositionService,
      LicencePositionViewService licencePositionViewService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicenceService licenceService,
      CorrectionDetailsViewService correctionDetailsViewService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionViewService = licencePositionViewService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
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
      return switch (licencePositionViewService.getFilteredCorrectionTimeline(licenceCorrection, filter)) {
        case FilteredCorrectionTimeline.LatestMatchingPosition(String url) ->
            ReverseRouter.redirectToUrl(url);
        case FilteredCorrectionTimeline.NoMatchingPositions(LicencePositionPageView pageView) ->
            licencePositionsModelAndView(licenceCorrection, pageView, filter);
      };
    }

    var licence = licenceCorrection.getLicence();
    var executedLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    var addedPositions = licencePositionCorrectionService.getAddedLicencePositionCorrections(licenceCorrection);

    if (executedLicencePositions.isEmpty() && addedPositions.isEmpty()) {
      return licencePositionsModelAndView(licenceCorrection, LicencePositionPageView.empty(), filter);
    }

    if (!executedLicencePositions.isEmpty()) {
      return ReverseRouter.redirect(on(this.getClass()).renderLicencePosition(
          licenceCorrection, executedLicencePositions.getLast(), null));
    }

    return ReverseRouter.redirect(on(this.getClass())
        .renderAddedPosition(licenceCorrection, addedPositions.getLast(), null));
  }

  @GetMapping("/{correctionId}/{licencePositionId}")
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderLicencePosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession
  ) {
    if (licenceCorrection.isComplete()) {
      return appliedCorrectionRedirect(licenceCorrection);
    }

    var filter = getFilter(licenceCorrection, filterSession);
    var licencePositionPageView =
        licencePositionViewService.getCorrectionPositionPageView(licenceCorrection, licencePosition, filter);

    return licencePositionsModelAndView(licenceCorrection, licencePositionPageView, filter);
  }

  @GetMapping("/{correctionId}/added-positions/{licencePositionCorrectionId}")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderAddedPosition(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession
  ) {
    if (licenceCorrection.isComplete()) {
      return appliedCorrectionRedirect(licenceCorrection);
    }

    var filter = getFilter(licenceCorrection, filterSession);
    var licencePositionPageView = licencePositionViewService.getCorrectionAddedPositionPageView(
        licenceCorrection,
        licencePositionCorrection,
        filter
    );

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
        LicenceTimelineFilter.from(form, licencePositionViewService.getCorrectionFilterOptions(licenceCorrection))
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