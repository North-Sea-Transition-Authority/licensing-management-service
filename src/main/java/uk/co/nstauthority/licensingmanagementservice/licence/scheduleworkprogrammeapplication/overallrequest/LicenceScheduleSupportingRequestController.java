package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

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
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/supporting-information")
public class LicenceScheduleSupportingRequestController {

  public static final String PAGE_TITLE = "Supporting information";
  private final LicenceScheduleSupportingRequestService licenceScheduleSupportingRequestService;
  private final LicenceScheduleSupportingRequestFormValidator licenceScheduleSupportingRequestFormValidator;

  public LicenceScheduleSupportingRequestController(
      LicenceScheduleSupportingRequestService licenceScheduleSupportingRequestService,
      LicenceScheduleSupportingRequestFormValidator licenceScheduleSupportingRequestFormValidator
  ) {
    this.licenceScheduleSupportingRequestService = licenceScheduleSupportingRequestService;
    this.licenceScheduleSupportingRequestFormValidator = licenceScheduleSupportingRequestFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(
        licenceScheduleSupportingRequestService.getLicenceScheduleRequestForm(scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping
  ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") LicenceScheduleSupportingRequestForm form,
      BindingResult bindingResult
  ) {

    if (!licenceScheduleSupportingRequestFormValidator.isValid(bindingResult, scheduleWorkProgrammeApplicationDetail)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    licenceScheduleSupportingRequestService.saveRequestForm(form, scheduleWorkProgrammeApplicationDetail);

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, scheduleWorkProgrammeApplicationDetail, null));
  }

  private ModelAndView getModelAndView(
      LicenceScheduleSupportingRequestForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    var modelAndView = new ModelAndView(
        "lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceSupportingInformationRequest");
    modelAndView.addObject("pageTitle", PAGE_TITLE)
                .addObject("form", form)
                .addObject("isExtension", licenceScheduleSupportingRequestService.isExtensionOrAmendment(
                    scheduleWorkProgrammeApplicationDetail))
        .addObject("cancelUrl", ReverseRouter.route(
            on(ScheduleWorkProgrammeApplicationTaskListController.class)
                .getTaskList(
                    scheduleWorkProgrammeApplicationDetail.getId(),
                    null,
                    null
                ))
        );


    return modelAndView;
  }
}