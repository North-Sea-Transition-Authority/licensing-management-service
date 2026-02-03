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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;


@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/" +
    "work-programme-amendment/{workProgrammeActivityId}/details")
@ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
public class LicenceWorkProgrammeAmendmentController {

  public static final String PAGE_TITLE = "Work programme amendments";
  private final LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;
  private final LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;
  private final WorkProgrammeActivityService  workProgrammeActivityService;

  public LicenceWorkProgrammeAmendmentController(
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService,
      LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
    this.licenceWorkProgrammeAmendmentFormValidator = licenceWorkProgrammeAmendmentFormValidator;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity workProgrammeActivity,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(
        workProgrammeActivity,
        licenceWorkProgrammeAmendmentService.getLicenceWorkProgrammeActivityAmendmentForm(
            workProgrammeActivity,
            scheduleWorkProgrammeApplicationDetail
        ),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping
  ModelAndView submitForm(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity workProgrammeActivity,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") LicenceWorkProgrammeAmendmentForm form,
      BindingResult bindingResult
  ) {
    if (!licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(workProgrammeActivity, form, scheduleWorkProgrammeApplicationDetail);
    }

    licenceWorkProgrammeAmendmentService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail, workProgrammeActivity);

    return ReverseRouter.redirect(on(LicenceWorkProgrammeAmendmentSummaryController.class)
        .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
  }

  private ModelAndView getModelAndView(
      WorkProgrammeActivity workProgrammeActivity,
      LicenceWorkProgrammeAmendmentForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendment")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("workProgrammeActivityDetails",
                   workProgrammeActivityService.createWorkProgrammeActivityView(workProgrammeActivity)
        )
        .addObject("isLinkedRelativeDate",
            workProgrammeActivity.getDateOption().equals(WorkProgrammeActivityDateOption.RELATIVE_DATE)
        )
        .addObject("cancelUrl", ReverseRouter.route(
            on(LicenceWorkProgrammeAmendmentSummaryController.class)
                .renderForm(scheduleWorkProgrammeApplicationDetail.getId(), null)));
  }
}