package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;

@Controller
@RequestMapping("/licence/schedule/term/{licenceScheduleTermId}/delete")
public class LicenceScheduleTermDeletionController {

  private static final String PAGE_TITLE = "Do you want to delete the %s?";

  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;

  public LicenceScheduleTermDeletionController(
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceScheduleCalculationService licenceScheduleCalculationService
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
  }

  @GetMapping
  public ModelAndView renderDeleteTermPage(
      @PathVariable UUID licenceScheduleTermId
  ) {
    var term = licenceScheduleTermService.getTermByIdOrThrow(licenceScheduleTermId);

    return new ModelAndView("lms/licence/schedule/deleteScheduleTerm")
        .addObject("pageTitle", PAGE_TITLE.formatted(term.getTermType().getDisplayName()))
        .addObject("licenceScheduleTermSummaryView", LicenceScheduleTermSummaryView.fromTerm(term))
        .addObject("cancelUrl", term.getLicenceScheduleDetail().getScheduleTimelineRouteUrl());
  }

  @PostMapping
  public ModelAndView submitDeleteTermPage(
      @PathVariable UUID licenceScheduleTermId
  ) {
    var term = licenceScheduleTermService.getTermByIdOrThrow(licenceScheduleTermId);
    var licenceScheduleDetail = term.getLicenceScheduleDetail();
    licenceScheduleTermService.deleteTerm(term);

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

}
