package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.HasAnyRoleInTeamTypeInterceptorRule.HasAnyRoleInTeamType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchTabItem;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("document-library/templates")
@SessionAttributes("documentTemplateSearchSession")
@HasAnyRoleInTeamType(TeamType.LICENCE_MANAGEMENT)
public class DocumentTemplateSearchController {

  private static final String DEFAULT_TAB = "continuation";

  private final DocumentTemplateSearchService documentTemplateSearchService;
  private final DocumentTemplateSearchTabService documentTemplateSearchTabService;

  @Autowired
  public DocumentTemplateSearchController(DocumentTemplateSearchService documentTemplateSearchService,
                                          DocumentTemplateSearchTabService documentTemplateSearchTabService) {
    this.documentTemplateSearchService = documentTemplateSearchService;
    this.documentTemplateSearchTabService = documentTemplateSearchTabService;
  }

  @GetMapping
  public ModelAndView renderDocumentTemplateSearch(
      @ModelAttribute("documentTemplateSearchSession") DocumentTemplateSearchSession documentTemplateSearchSession,
      @RequestParam(name = "page", defaultValue = "0") Integer pageNumber,
      @RequestParam(name = "tab", defaultValue = DEFAULT_TAB) DocumentTemplateSearchTab tab
  ) {
    return getModelAndViewForDocumentTemplate(documentTemplateSearchSession.getSearchFilterForm(), pageNumber, tab);
  }

  @PostMapping
  public ModelAndView renderDocumentTemplateResults(
      @ModelAttribute("form") DocumentTemplateSearchFilterForm form,
      @ModelAttribute("documentTemplateSearchSession") DocumentTemplateSearchSession documentTemplateSearchSession,
      @RequestParam(name = "tab", defaultValue = DEFAULT_TAB) DocumentTemplateSearchTab tab) {

    documentTemplateSearchSession.update(form);
    return ReverseRouter.redirect(on(DocumentTemplateSearchController.class)
        .renderDocumentTemplateSearch(null, null, tab));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearDocumentTemplateSearchFilters(
      @ModelAttribute("documentTemplateSearchSession") DocumentTemplateSearchSession documentTemplateSearchSession,
      @RequestParam(name = "tab", defaultValue = DEFAULT_TAB) DocumentTemplateSearchTab tab,
      SessionStatus sessionStatus) {
    sessionStatus.setComplete();
    documentTemplateSearchSession.clearFilters();
    return ReverseRouter.redirect(on(DocumentTemplateSearchController.class)
        .renderDocumentTemplateSearch(null, null, tab));
  }

  @ModelAttribute("documentTemplateSearchSession")
  private DocumentTemplateSearchSession getDocumentTemplateSearchSessionWithDefaultFilters(
      @ModelAttribute("form") DocumentTemplateSearchFilterForm form) {
    return new DocumentTemplateSearchSession(form);
  }

  private ModelAndView getModelAndViewForDocumentTemplate(DocumentTemplateSearchFilterForm form,
                                                          int pageNumber,
                                                          DocumentTemplateSearchTab tab
  ) {
    var filteredItems = documentTemplateSearchService.getDocumentTemplateSearchItems(form);
    var searchItemsByTab = documentTemplateSearchTabService.getSearchTabItems(filteredItems, pageNumber, tab);
    var tabs = searchItemsByTab.stream().map(SearchTabItem::tabView).toList();
    return new ModelAndView("lms/document/documentTemplateSearchPage")
        .addObject("form", form)
        .addObject("licenceTypes", DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()))
        .addObject("searchItemsByTab", searchItemsByTab)
        .addObject("tabs", tabs)
        .addObject("selectedTab", tab)
        .addObject("clearFilterUrl", ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .clearDocumentTemplateSearchFilters(null, tab, null)))
        .addObject("controllerUrl", ReverseRouter.route(on(DocumentTemplateSearchController.class)
            .renderDocumentTemplateSearch(null, null, null)));
  }
}