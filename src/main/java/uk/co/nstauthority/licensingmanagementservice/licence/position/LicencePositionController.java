package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterForm;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterSession;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/{licenceId}/timeline")
@SessionAttributes(LicencePositionController.FILTER_SESSION_ATTRIBUTE)
public class LicencePositionController {

  static final String FILTER_SESSION_ATTRIBUTE = "licenceTimelineFilterSession";

  private final LicenceTimelinePositionTab licenceTimelinePositionTab;
  private final TabbedLicencePageService tabbedLicencePageService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionViewService licencePositionViewService;

  LicencePositionController(
      LicenceTimelinePositionTab licenceTimelinePositionTab,
      TabbedLicencePageService tabbedLicencePageService,
      LicencePositionService licencePositionService,
      LicencePositionViewService licencePositionViewService
  ) {
    this.licenceTimelinePositionTab = licenceTimelinePositionTab;
    this.tabbedLicencePageService = tabbedLicencePageService;
    this.licencePositionService = licencePositionService;
    this.licencePositionViewService = licencePositionViewService;
  }

  @GetMapping
  public ModelAndView renderLicencePositionTimeline(
      Licence licence,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession,
      ServiceUserDetail user
  ) {
    var changeTypes = filterSession.getChangeTypes(licence.getLicenceReference());
    var licencePositionPageView = licencePositionViewService.getLatestPositionPageView(licence, changeTypes);

    return licencePositionsModelAndView(licence, licencePositionPageView, changeTypes, user);
  }

  @GetMapping("/{licencePositionId}")
  public ModelAndView renderLicencePosition(
      Licence licence,
      @PathVariable UUID licencePositionId,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession,
      ServiceUserDetail user
  ) {
    var changeTypes = filterSession.getChangeTypes(licence.getLicenceReference());
    var licencePosition = licencePositionService.getPositionForLicence(licence, licencePositionId);
    var licencePositionPageView = licencePositionViewService.getPositionPageView(licencePosition, changeTypes);

    return licencePositionsModelAndView(licence, licencePositionPageView, changeTypes, user);
  }

  @PostMapping
  public ModelAndView filterLicencePositionTimeline(
      Licence licence,
      @ModelAttribute("form") LicenceTimelineFilterForm form,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession
  ) {
    // Only the change types on this licence can be filtered by, so anything else submitted is dropped.
    var availableChangeTypes = licencePositionViewService.getChangeTypeOptions(licence).keySet();
    var changeTypes = form.getChangeTypes().stream()
        .filter(availableChangeTypes::contains)
        .collect(Collectors.toSet());

    filterSession.update(licence.getLicenceReference(), changeTypes);

    return ReverseRouter.redirect(on(LicencePositionController.class)
        .renderLicencePositionTimeline(licence, null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearLicencePositionTimelineFilters(
      Licence licence,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession,
      SessionStatus sessionStatus
  ) {
    filterSession.clear(licence.getLicenceReference());

    if (filterSession.isEmpty()) {
      sessionStatus.setComplete();
    }

    return ReverseRouter.redirect(on(LicencePositionController.class)
        .renderLicencePositionTimeline(licence, null, null));
  }

  @ModelAttribute(FILTER_SESSION_ATTRIBUTE)
  LicenceTimelineFilterSession getFilterSession() {
    return new LicenceTimelineFilterSession();
  }

  private ModelAndView licencePositionsModelAndView(
      Licence licence,
      LicencePositionPageView licencePositionPageView,
      List<String> changeTypes,
      ServiceUserDetail user
  ) {
    var form = new LicenceTimelineFilterForm();
    form.setChangeTypes(changeTypes);

    var licencePositionsModelAndView = new ModelAndView("lms/licence/position/licencePositions")
        .addObject("licencePositionPageView", licencePositionPageView)
        .addObject("form", form)
        .addObject("filterUrl", ReverseRouter.route(on(LicencePositionController.class)
            .filterLicencePositionTimeline(licence, null, null)))
        .addObject("clearFilterUrl", ReverseRouter.route(on(LicencePositionController.class)
            .clearLicencePositionTimelineFilters(licence, null, null)));

    tabbedLicencePageService.hydrateModel(licencePositionsModelAndView, licence, licenceTimelinePositionTab, user);

    return licencePositionsModelAndView;
  }
}
