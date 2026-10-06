package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToLicence;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
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
  private final LicencePositionViewService licencePositionViewService;

  LicencePositionController(
      LicenceTimelinePositionTab licenceTimelinePositionTab,
      TabbedLicencePageService tabbedLicencePageService,
      LicencePositionViewService licencePositionViewService
  ) {
    this.licenceTimelinePositionTab = licenceTimelinePositionTab;
    this.tabbedLicencePageService = tabbedLicencePageService;
    this.licencePositionViewService = licencePositionViewService;
  }

  @GetMapping
  public ModelAndView renderLicencePositionTimeline(
      Licence licence,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession,
      ServiceUserDetail user
  ) {
    var filter = filterSession.getFilter(licence.getLicenceReference());
    var licencePositionPageView = licencePositionViewService.getLatestPositionPageView(licence, filter);

    return licencePositionsModelAndView(licence, licencePositionPageView, filter, user);
  }

  @GetMapping("/{licencePositionId}")
  @LicencePositionBelongsToLicence
  public ModelAndView renderLicencePosition(
      Licence licence,
      LicencePosition licencePosition,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession,
      ServiceUserDetail user
  ) {
    var filter = filterSession.getFilter(licence.getLicenceReference());
    var licencePositionPageView = licencePositionViewService.getPositionPageView(licencePosition, filter);

    return licencePositionsModelAndView(licence, licencePositionPageView, filter, user);
  }

  @PostMapping
  public ModelAndView filterLicencePositionTimeline(
      Licence licence,
      @ModelAttribute("form") LicenceTimelineFilterForm form,
      @ModelAttribute(FILTER_SESSION_ATTRIBUTE) LicenceTimelineFilterSession filterSession
  ) {
    // Only the change types and organisations on this licence can be filtered by, so anything else is dropped.
    var filterOptions = licencePositionViewService.getFilterOptions(licence);
    var changeTypes = form.getChangeTypes().stream()
        .filter(filterOptions.changeTypeOptions()::containsKey)
        .collect(Collectors.toSet());
    var organisationIds = form.getOrganisationIds().stream()
        .filter(Objects::nonNull)
        .filter(organisationId -> filterOptions.organisationOptions().containsKey(String.valueOf(organisationId)))
        .collect(Collectors.toSet());

    filterSession.update(licence.getLicenceReference(), new LicenceTimelineFilter(changeTypes, organisationIds));

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
      LicenceTimelineFilter filter,
      ServiceUserDetail user
  ) {
    var form = new LicenceTimelineFilterForm();
    form.setChangeTypes(List.copyOf(filter.changeTypes()));
    form.setOrganisationIds(List.copyOf(filter.organisationIds()));

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
