package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;


@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/" +
    "work-programme-amendment/{workProgrammeActivityId}/details")
public class LicenceWorkProgrammeAmendmentController {

  public static final String PAGE_TITLE = "Work programme amendments";
  private final LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;
  private final LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  public LicenceWorkProgrammeAmendmentController(
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService,
      LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator
  ) {
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
    this.licenceWorkProgrammeAmendmentFormValidator = licenceWorkProgrammeAmendmentFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID workProgrammeActivityId,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(
        licenceWorkProgrammeAmendmentService.getLicenceWorkProgrammeActivityAmendmentForm(
            workProgrammeActivityId,
            scheduleWorkProgrammeApplicationDetail),
            scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping
  ModelAndView submitForm(
      @PathVariable UUID workProgrammeActivityId,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") LicenceWorkProgrammeAmendmentForm form,
      BindingResult bindingResult
  ) {
    if (!licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail,
        workProgrammeActivityId);

    return ReverseRouter.redirect(on(LicenceWorkProgrammeAmendmentSummaryController.class)
        .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
  }

  private ModelAndView getModelAndView(LicenceWorkProgrammeAmendmentForm form,
                                       ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    UUID scheduleWorkProgrammeApplicationDetailId = scheduleWorkProgrammeApplicationDetail.getId();
    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendment")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("isLinkedToPhaseOrTerm", isLinkedToPhaseOrTerm())
        .addObject("cancelUrl", ReverseRouter.route(
            on(LicenceWorkProgrammeAmendmentSummaryController.class)
                .renderForm(scheduleWorkProgrammeApplicationDetailId, null)));
  }

  public boolean isLinkedToPhaseOrTerm() {
    return true;
  }
}