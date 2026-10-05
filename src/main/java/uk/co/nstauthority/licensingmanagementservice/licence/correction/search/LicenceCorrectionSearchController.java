package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.IsMemberOfRegulatorTeam;

@Controller
@RequestMapping("/licence-corrections/search")
@IsMemberOfRegulatorTeam
public class LicenceCorrectionSearchController {

  private final LicenceCorrectionSearchService licenceCorrectionSearchService;

  LicenceCorrectionSearchController(LicenceCorrectionSearchService licenceCorrectionSearchService) {
    this.licenceCorrectionSearchService = licenceCorrectionSearchService;
  }

  @GetMapping
  public ModelAndView renderCorrectionSearch(ServiceUserDetail user) {
    return new ModelAndView("lms/licence/correction/search/correctionSearch")
        .addObject("searchItems", licenceCorrectionSearchService.getSearchItems(user));
  }
}