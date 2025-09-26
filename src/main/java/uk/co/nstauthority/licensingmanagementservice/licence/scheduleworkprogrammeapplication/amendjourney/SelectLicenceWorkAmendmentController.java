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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping
    ("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/work-programme-amendment")
public class SelectLicenceWorkAmendmentController {

  public static final String PAGE_TITLE = "What work programme activity are you requesting to amend?";
  private final SelectLicenceAmendmentFormValidator selectLicenceAmendmentFormValidator;
  private final  SelectLicenceAmendmentFormService selectLicenceAmendmentFormService;

  public SelectLicenceWorkAmendmentController(
      SelectLicenceAmendmentFormValidator selectLicenceAmendmentFormValidator,
      SelectLicenceAmendmentFormService selectLicenceAmendmentFormService
  ) {
    this.selectLicenceAmendmentFormValidator = selectLicenceAmendmentFormValidator;
    this.selectLicenceAmendmentFormService = selectLicenceAmendmentFormService;
  }

  @GetMapping("/create")
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(
        new SelectLicenceAmendmentForm(),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping("/create")
  ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") SelectLicenceAmendmentForm form,
      BindingResult bindingResult
  ) {
    if (!selectLicenceAmendmentFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    selectLicenceAmendmentFormService
        .saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail);

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null));
  }

  private ModelAndView getModelAndView(SelectLicenceAmendmentForm form,
                                       ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {


    UUID scheduleWorkProgrammeApplicationDetailId = scheduleWorkProgrammeApplicationDetail.getId();
    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/selectScheduleWorkProgrammeToAmend")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("workProgrammeAmendments", MockWorkAmendment.getMockWorkAmendments())
        .addObject("cancelUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null
            )));

  }
}