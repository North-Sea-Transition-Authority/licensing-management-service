package uk.co.nstauthority.template.xyzapplication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.template.fds.searchselector.RestSearchResult;
import uk.co.nstauthority.template.fds.searchselector.SearchSelectorService;

@RestController("/api")
public class XyzApplicationRestController {

  private final XyzApplicationService xyzApplicationService;
  private final SearchSelectorService searchSelectorService;

  public XyzApplicationRestController(XyzApplicationService xyzApplicationService,
                                      SearchSelectorService searchSelectorService) {
    this.xyzApplicationService = xyzApplicationService;
    this.searchSelectorService = searchSelectorService;
  }

  @GetMapping("/applications")
  public RestSearchResult searchApplications(@RequestParam(value = "term") String term) {
    return searchSelectorService.search(term, xyzApplicationService::getXyzApplicationsByReference);
  }
}
