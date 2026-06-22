package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDeleteController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping("licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/task-list")
@ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
@LogWorkAreaItemView(
    itemType = WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION,
    pathVariable = "scheduleWorkProgrammeApplicationDetailId"
)
public class ScheduleWorkProgrammeApplicationTaskListController {

  public static final String PAGE_TITLE = "Task list";

  private final ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;
  private final LicenceService licenceService;

  public ScheduleWorkProgrammeApplicationTaskListController(
      ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService,
      LicenceService licenceService
  ) {
    this.scheduleWorkProgrammeApplicationTaskListService = scheduleWorkProgrammeApplicationTaskListService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView getTaskList(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail serviceUserDetail) {

    var sections = scheduleWorkProgrammeApplicationTaskListService.getAllSections(
        scheduleWorkProgrammeApplicationDetail,
        serviceUserDetail
    );

    var licence = scheduleWorkProgrammeApplicationDetail.getLicence();

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/taskList")
        .addObject("taskListSections", sections)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("deleteScheduleWorkProgrammeApplicationUrl", ReverseRouter.route(on(
            ScheduleWorkProgrammeApplicationDeleteController.class).renderForm(
                scheduleWorkProgrammeApplicationDetailId,
                null)));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}