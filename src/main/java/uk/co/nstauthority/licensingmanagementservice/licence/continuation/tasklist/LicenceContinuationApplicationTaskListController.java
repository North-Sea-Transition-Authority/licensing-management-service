package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/task-list")
// TODO restrict by status
public class LicenceContinuationApplicationTaskListController {

  public static final String PAGE_TITLE = "Task list";

  private final LicenceService licenceService;

  public LicenceContinuationApplicationTaskListController(LicenceService licenceService) {
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView getTaskList(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {

    var modelAndView = new ModelAndView("lms/licence/continuation/taskList")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("taskListSections", List.of())
        .addObject("pageCaption",
            licenceService.getLicencePageCaption(getLicence(licenceContinuationApplicationDetail)));

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  private Licence getLicence(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    return licenceContinuationApplicationDetail.getLicenceContinuationApplication()
        .getLicenceScheduleDetail().getLicenceSchedule()
        .getLicence();
  }
}