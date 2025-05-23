package uk.co.nstauthority.licensingmanagementservice.xyzapplication.tasklist;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;

@Controller
@RequestMapping("/application/{applicationId}/task-list")
public class XyzApplicationTaskListController {

  public static final String PAGE_TITLE = "Task list";

  private final XyzApplicationTaskListService xyzApplicationTaskListService;

  public XyzApplicationTaskListController(XyzApplicationTaskListService xyzApplicationTaskListService) {
    this.xyzApplicationTaskListService = xyzApplicationTaskListService;
  }

  @GetMapping
  public ModelAndView getTaskList(@PathVariable("applicationId") UUID applicationId,
                                  XyzApplication xyzApplication,
                                  ServiceUserDetail serviceUserDetail) {
    var sections = xyzApplicationTaskListService.getAllSections(xyzApplication, serviceUserDetail);

    var modelAndView = new ModelAndView("lms/application/taskList")
        .addObject("taskListSections", sections)
        .addObject("pageTitle", PAGE_TITLE);

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}
