package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import java.util.HashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.RestSearchResult;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;

@RestController
@RequestMapping("/api")
public class OrganisationUnitRestController {

  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final SearchSelectorService searchSelectorService;

  @Autowired
  public OrganisationUnitRestController(
      OrganisationUnitQueryService organisationUnitQueryService,
      SearchSelectorService searchSelectorService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.searchSelectorService = searchSelectorService;
  }

  @GetMapping("/organisation-units")
  public RestSearchResult searchOrganisationUnits(@RequestParam(value = "term") String term) {
    return searchSelectorService.search(
        term,
        organisationUnitQueryService.searchOrganisationUnitsWithName(term)
    );
  }

  @GetMapping("/organisation-units/search-constraints")
  public RestSearchResult searchOrganisationUnitsWithConstraints(
      @RequestParam(value = "term") String term,
      @RequestParam(value = "include", defaultValue = "") List<String> includeNames,
      @RequestParam(value = "exclude", defaultValue = "") List<String> excludeNames
  ) {
    var includeNameSet = new HashSet<>(includeNames);
    var excludeNameSet = new HashSet<>(excludeNames);
    return searchSelectorService.search(
        term,
        organisationUnitQueryService.searchOrganisationUnitsWithNameCompareToList(term, includeNameSet, excludeNameSet)
    );
  }
}
