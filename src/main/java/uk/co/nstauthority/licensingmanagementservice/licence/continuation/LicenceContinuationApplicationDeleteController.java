package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit.ContinuationSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licence/continuation-application/{licenceContinuationApplicationDetailId}/delete-application")
@ContinuationApplicationHasStatus(value = LicenceContinuationApplicationStatus.DRAFT)
@InvokingUserCanAccessContinuationApplication
public class LicenceContinuationApplicationDeleteController {

  private final LicenceContinuationService licenceContinuationService;
  private final ContinuationSummarySectionService continuationSummarySectionService;

  public LicenceContinuationApplicationDeleteController(
      LicenceContinuationService licenceContinuationService,
      ContinuationSummarySectionService continuationSummarySectionService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.continuationSummarySectionService = continuationSummarySectionService;
  }

  @GetMapping()
  public ModelAndView renderForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail serviceUserDetail
  ) {
    return getModelAndView(licenceContinuationApplicationDetailId, licenceContinuationApplicationDetail, serviceUserDetail);
  }

  @PostMapping()
  public ModelAndView deleteLicenceContinuationApplication(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      RedirectAttributes redirectAttributes
  ) {
    licenceContinuationService.deleteLicenceContinuationApplication(licenceContinuationApplicationDetail);
    addRedirectNotification(redirectAttributes);
    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  private ModelAndView getModelAndView(
      UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail serviceUserDetail
  ) {
    var taskListUrl = ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
        .getTaskList(licenceContinuationApplicationDetailId, null, null));

    var modelAndView = new ModelAndView("lms/licence/continuation/licenceContinuationApplicationDeleteConfirmation")
        .addObject("backToTaskListUrl", taskListUrl)
        .addObject("actionUrl", ReverseRouter.route(on(
                LicenceContinuationApplicationDeleteController.class).deleteLicenceContinuationApplication(
                licenceContinuationApplicationDetailId, null, null)))
        .addObject("summarySections", continuationSummarySectionService.getSummarySections(
                licenceContinuationApplicationDetail, serviceUserDetail))
        .addObject("accordionId", licenceContinuationApplicationDetailId);

    var breadcrumbs = Breadcrumbs.builder("Are you sure you want to delete this application?")
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);
    return modelAndView;
  }

  private void addRedirectNotification(
      RedirectAttributes redirectAttributes) {
    NotificationBanner.newSuccessBannerWithHeader(
        String.format(
            "%s has been deleted",
            ApplicationType.CONTINUATION_APPLICATION.getDisplayName()
        ),
        redirectAttributes
    );
  }
}
