package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collections;
import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceController;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.search.action.LicenceActionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("licences/search")
@SessionAttributes("licenceSearchSession")
public class LicenceSearchController {

  private final LicenceSearchService licenceSearchService;
  private final LicenceActionService licenceActionService;
  private final LicenceAccessService licenceAccessService;

  public LicenceSearchController(
      LicenceSearchService licenceSearchService,
      LicenceActionService licenceActionService,
      LicenceAccessService licenceAccessService
  ) {
    this.licenceSearchService = licenceSearchService;
    this.licenceActionService = licenceActionService;
    this.licenceAccessService = licenceAccessService;
  }

  @GetMapping
  public ModelAndView renderSearchPage(
      @ModelAttribute("licenceSearchSession") LicenceSearchSession searchSession,
      ServiceUserDetail serviceUserDetail
  ) {
    var form = searchSession.getSearchFilterForm();

    List<SearchResultItem> licenceSearchItems;
    boolean hasSearchBeenInvoked = searchSession.hasSearchBeenInvoked();
    if (!hasSearchBeenInvoked) {
      form.setLicenceTypes(LicenceType.getDisplayableLicenceTypesNames());
      licenceSearchItems = Collections.emptyList();
    } else {
      licenceSearchItems = licenceSearchService.getSearchResultItems(form);
    }

    return getLicenceSearchModelAndView(form, licenceSearchItems, hasSearchBeenInvoked, serviceUserDetail);
  }

  @PostMapping
  public ModelAndView filterResults(@ModelAttribute("form") LicenceSearchFilterForm form,
                                    @ModelAttribute("licenceSearchSession") LicenceSearchSession searchSession) {
    searchSession.update(form);
    return ReverseRouter.redirect(on(LicenceSearchController.class).renderSearchPage(null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearSearchFilters(@ModelAttribute("licenceSearchSession") LicenceSearchSession searchSession,
                                         SessionStatus sessionStatus) {
    sessionStatus.setComplete();
    return ReverseRouter.redirect(on(LicenceSearchController.class).renderSearchPage(null, null));
  }

  @ModelAttribute("licenceSearchSession")
  private LicenceSearchSession getSearchSession(@ModelAttribute("form") LicenceSearchFilterForm form) {
    return new LicenceSearchSession(form);
  }

  // Only here for now while we haven't got a page to go to from search
  @GetMapping("/{licenceId}")
  public ModelAndView renderLicenceOverview(
      @PathVariable Integer licenceId,
      Licence licence,
      ServiceUserDetail serviceUserDetail
  ) {
    return new ModelAndView("lms/licence/search/licenceOverview")
        .addObject("licenceReference", licence.getLicenceReference())
        .addObject("caption", licence.getType().getDisplayName())
        .addObject("licenceActions", licenceActionService.getAvailableUserActionItems(licence, serviceUserDetail));
  }

  private ModelAndView getLicenceSearchModelAndView(LicenceSearchFilterForm form,
                                                    List<SearchResultItem> searchItems,
                                                    boolean hasSearchBeenInvoked,
                                                    ServiceUserDetail user
  ) {
    return new ModelAndView("lms/licence/search/licenceSearch")
        .addObject("form", form)
        .addObject("clearFilterUrl", ReverseRouter.route(on(LicenceSearchController.class).clearSearchFilters(null, null)))
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("licenseeOrgUnitUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)))
        .addObject("preSelectedLicenseeOrgUnit", licenceSearchService.getPreselectedOrganisationUnit(form.getLicenseeOrgUnitId()))
        .addObject("searchItems", searchItems)
        .addObject("hasSearchBeenInvoked", hasSearchBeenInvoked)
        .addObject("canCreateLicence", licenceAccessService.userHasAccessToCreateLicence((user.wuaId())))
        .addObject("createLicenceUrl", ReverseRouter.route(on(LicenceController.class).renderNewLicenceForm()));
  }
}