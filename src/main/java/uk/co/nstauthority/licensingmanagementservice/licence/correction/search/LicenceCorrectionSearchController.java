package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.IsMemberOfRegulatorTeam;

@Controller
@RequestMapping("/licence-corrections/search")
@IsMemberOfRegulatorTeam
public class LicenceCorrectionSearchController {

  @GetMapping
  public ModelAndView renderCorrectionSearch() {
    return new ModelAndView("lms/licence/correction/search/correctionSearch")
        .addObject("searchItems", List.of());
  }
}