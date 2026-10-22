package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.RestSearchResult;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;

@RestController
@RequestMapping("/api/mockups")
@Profile("mockups")
public class DecisionDetailsMockupRestController {

  private final SearchSelectorService searchSelectorService;

  public DecisionDetailsMockupRestController(SearchSelectorService searchSelectorService) {
    this.searchSelectorService = searchSelectorService;
  }

  @GetMapping("/raci-consultees")
  public RestSearchResult searchRaciConsultees(@RequestParam("term") String term) {
    return searchSelectorService.search(term, DecisionDetailsMockupData.RACI_CONSULTEES);
  }
}
