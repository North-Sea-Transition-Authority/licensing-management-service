package uk.co.nstauthority.licensingmanagementservice.licence.search;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Controller
@RequestMapping("licences/search")
public class LicenceSearchController {

  private final LicenceSearchService licenceSearchService;

  public LicenceSearchController(LicenceSearchService licenceSearchService) {
    this.licenceSearchService = licenceSearchService;
  }

  @GetMapping
  public ModelAndView renderLicenceSearchPage() {
    return getLicenceSearchModelAndView();
  }

  private ModelAndView getLicenceSearchModelAndView() {
    return new ModelAndView("lms/licence/search/licenceSearch")
        .addObject("searchItems", licenceSearchService.getSearchResultItems())
        .addObject("form", new LicenceSearchFilterForm());
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

}
