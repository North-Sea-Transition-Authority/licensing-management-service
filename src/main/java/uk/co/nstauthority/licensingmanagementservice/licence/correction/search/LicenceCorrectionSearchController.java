package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/licence-corrections/search")
public class LicenceCorrectionSearchController {

  @GetMapping
  public ModelAndView renderCorrectionSearch() {
    return new ModelAndView("lms/licence/correction/search/correctionSearch")
        .addObject("searchItems", List.of());
  }
}