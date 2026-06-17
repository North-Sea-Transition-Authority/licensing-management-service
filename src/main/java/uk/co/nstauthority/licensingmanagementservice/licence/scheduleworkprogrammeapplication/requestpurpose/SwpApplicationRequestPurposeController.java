package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.RequestPurposeChoiceMustBeApplicable;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Controller
@RequestMapping("licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/request-purpose")
@ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
@RequestPurposeChoiceMustBeApplicable
public class SwpApplicationRequestPurposeController {

  static final String PAGE_TITLE = "What are you requesting to do?";

  private final SwpApplicationRequestPurposeValidator swpApplicationRequestPurposeValidator;
  private final SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

  public SwpApplicationRequestPurposeController(
      SwpApplicationRequestPurposeValidator swpApplicationRequestPurposeValidator,
      SwpApplicationRequestPurposeService swpApplicationRequestPurposeService
  ) {
    this.swpApplicationRequestPurposeValidator = swpApplicationRequestPurposeValidator;
    this.swpApplicationRequestPurposeService = swpApplicationRequestPurposeService;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(
        swpApplicationRequestPurposeService.getFilledSwpApplicationRequestPurposeForm(
            scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") SwpApplicationRequestPurposeForm form,
      BindingResult bindingResult
  ) {
    if (!swpApplicationRequestPurposeValidator.isValid(form, bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }
    swpApplicationRequestPurposeService.saveOrUpdateRequestPurpose(scheduleWorkProgrammeApplicationDetail, form);

    return ReverseRouter.redirect(getTaskListRoute(scheduleWorkProgrammeApplicationDetailId));
  }

  private ModelAndView getTaskListRoute(UUID scheduleWorkProgrammeApplicationDetailId) {
    return on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, null, null);
  }

  private ModelAndView getModelAndView(SwpApplicationRequestPurposeForm form,
                                       ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/requestPurpose");

    var pageOptionsMap = swpApplicationRequestPurposeService.getPageOptions(
            scheduleWorkProgrammeApplicationDetail).stream()
        .collect(StreamUtil.toLinkedHashMap(Enum::name, SwpApplicationRequestPurposeOption::getDisplayName));

    var taskListUrl = ReverseRouter.route(getTaskListRoute(scheduleWorkProgrammeApplicationDetail.getId()));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageOptionsMap", pageOptionsMap)
        .addObject("cancelUrl", taskListUrl);
  }

}