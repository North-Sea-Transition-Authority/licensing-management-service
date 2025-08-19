package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Controller
@RequestMapping("licences/schedule-work-programme-applications/{scheduleWorkProgrammeApplicationDetailId}")
public class ScheduleWorkProgrammeApplicationTaskListController {

  public static final String PAGE_TITLE = "Task list";

  private final ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  public ScheduleWorkProgrammeApplicationTaskListController(
      ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService) {
    this.scheduleWorkProgrammeApplicationTaskListService = scheduleWorkProgrammeApplicationTaskListService;
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

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/taskList")
        .addObject("taskListSections", sections)
        .addObject("pageTitle", PAGE_TITLE);

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}
