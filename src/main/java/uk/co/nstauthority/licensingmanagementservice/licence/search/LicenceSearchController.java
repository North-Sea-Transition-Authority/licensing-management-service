package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("licences/search")
@SessionAttributes("licenceSearchSession")
public class LicenceSearchController {

  private final LicenceSearchService licenceSearchService;

  public LicenceSearchController(LicenceSearchService licenceSearchService) {
    this.licenceSearchService = licenceSearchService;
  }

  @GetMapping
  public ModelAndView renderSearchPage(@ModelAttribute("licenceSearchSession") LicenceSearchSession searchSession) {
    var form = searchSession.getSearchFilterForm();

    if (!searchSession.hasSearchBeenInvoked()) {
      form.setLicenceTypes(LicenceType.getDisplayableLicenceTypesNames());
    }

    return getLicenceSearchModelAndView(form);
  }

  @PostMapping
  public ModelAndView filterResults(@ModelAttribute("form") LicenceSearchFilterForm form,
                                    @ModelAttribute("licenceSearchSession") LicenceSearchSession searchSession) {
    searchSession.update(form);
    return ReverseRouter.redirect(on(LicenceSearchController.class).renderSearchPage(null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearSearchFilters(@ModelAttribute("licenceSearchSession") LicenceSearchSession searchSession,
                                         SessionStatus sessionStatus) {
    sessionStatus.setComplete();
    return ReverseRouter.redirect(on(LicenceSearchController.class).renderSearchPage(null));
  }

  @ModelAttribute("licenceSearchSession")
  private LicenceSearchSession getSearchSession(@ModelAttribute("form") LicenceSearchFilterForm form) {
    return new LicenceSearchSession(form);
  }

  // Only here for now while we haven't got a page to go to from search
  @GetMapping("/{licenceId}")
  ModelAndView renderLicenceOverview(
      @PathVariable Integer licenceId,
      Licence licence
  ) {
    return new ModelAndView("lms/licence/search/licenceOverview")
        .addObject("pageTitle", licence.getLicenceReference())
        .addObject("caption", licence.getType().getDisplayName());
  }

  private ModelAndView getLicenceSearchModelAndView(LicenceSearchFilterForm form) {
    return new ModelAndView("lms/licence/search/licenceSearch")
        .addObject("form", form)
        .addObject("clearFilterUrl", ReverseRouter.route(on(LicenceSearchController.class).clearSearchFilters(null, null)))
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", licenceSearchService.getPreselectedOrganisationUnit(form.getLicenseeOrgUnitId()))
        .addObject("searchItems", licenceSearchService.getSearchResultItems(form));
  }
}
