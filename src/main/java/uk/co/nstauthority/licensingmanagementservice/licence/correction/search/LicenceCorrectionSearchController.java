package uk.co.nstauthority.licensingmanagementservice.licence.correction.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.IsMemberOfRegulatorTeam;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/licence-corrections/search")
@SessionAttributes("licenceCorrectionSearchSession")
@IsMemberOfRegulatorTeam
public class LicenceCorrectionSearchController {

  private final LicenceCorrectionSearchService licenceCorrectionSearchService;

  LicenceCorrectionSearchController(LicenceCorrectionSearchService licenceCorrectionSearchService) {
    this.licenceCorrectionSearchService = licenceCorrectionSearchService;
  }

  @GetMapping
  public ModelAndView renderCorrectionSearch(
      @ModelAttribute("licenceCorrectionSearchSession") LicenceCorrectionSearchSession searchSession,
      ServiceUserDetail user
  ) {
    return new ModelAndView("lms/licence/correction/search/correctionSearch")
        .addObject("form", searchSession.getSearchFilterForm())
        .addObject("clearFilterUrl",
            ReverseRouter.route(on(LicenceCorrectionSearchController.class).clearSearchFilters(null, null)))
        .addObject("correctionStatuses", DisplayableEnumOptionUtil.getDisplayableOptions(
            List.of(LicenceCorrectionStatus.IN_PROGRESS, LicenceCorrectionStatus.COMPLETE)))
        .addObject("searchItems", licenceCorrectionSearchService.getSearchItems(searchSession.getSearchFilterForm(), user));
  }

  @PostMapping
  public ModelAndView filterResults(
      @ModelAttribute("form") LicenceCorrectionSearchFilterForm form,
      @ModelAttribute("licenceCorrectionSearchSession") LicenceCorrectionSearchSession searchSession
  ) {
    searchSession.update(form);
    return ReverseRouter.redirect(on(LicenceCorrectionSearchController.class).renderCorrectionSearch(null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearSearchFilters(
      @ModelAttribute("licenceCorrectionSearchSession") LicenceCorrectionSearchSession searchSession,
      SessionStatus sessionStatus
  ) {
    sessionStatus.setComplete();
    return ReverseRouter.redirect(on(LicenceCorrectionSearchController.class).renderCorrectionSearch(null, null));
  }

  @ModelAttribute("licenceCorrectionSearchSession")
  private LicenceCorrectionSearchSession getSearchSession(
      @ModelAttribute("form") LicenceCorrectionSearchFilterForm form
  ) {
    return new LicenceCorrectionSearchSession(form);
  }
}