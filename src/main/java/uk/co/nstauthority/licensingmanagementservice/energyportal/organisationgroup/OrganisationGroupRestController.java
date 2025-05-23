package uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.RestSearchResult;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;

@RestController
@RequestMapping("/api")
public class OrganisationGroupRestController {
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final SearchSelectorService searchSelectorService;

  public OrganisationGroupRestController(OrganisationGroupQueryService organisationGroupQueryService,
                                         SearchSelectorService searchSelectorService) {
    this.organisationGroupQueryService = organisationGroupQueryService;
    this.searchSelectorService = searchSelectorService;
  }

  @GetMapping("/organisation-group")
  public RestSearchResult getOrganisationGroupSearchResults(@RequestParam(value = "term", required = false) String term) {
    return searchSelectorService.search(term, organisationGroupQueryService::getOrganisationGroupsByName);
  }
}
