package uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/external-contributors")
@ContinuationApplicationHasStatus(value = LicenceContinuationApplicationStatus.DRAFT)
@InvokingUserCanAccessContinuationApplication
public class LicenceContinuationExternalContributorController {

  public static final String PAGE_TITLE = "External contributors";

  private final LicenceContinuationExternalContributorService licenceContinuationExternalContributorService;
  private final ExternalContributorFormValidator externalContributorFormValidator;

  public LicenceContinuationExternalContributorController(
      LicenceContinuationExternalContributorService licenceContinuationExternalContributorService,
      ExternalContributorFormValidator externalContributorFormValidator
  ) {
    this.licenceContinuationExternalContributorService = licenceContinuationExternalContributorService;
    this.externalContributorFormValidator = externalContributorFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return getModelAndView(
        licenceContinuationExternalContributorService.getExternalContributorForm(licenceContinuationApplicationDetail),
        licenceContinuationApplicationDetail
    );
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      @ModelAttribute("form") ExternalContributorForm form,
      BindingResult bindingResult
  ) {
    if (!externalContributorFormValidator.isValid(bindingResult)) {
      return getModelAndView(form, licenceContinuationApplicationDetail);
    }

    licenceContinuationExternalContributorService.saveExternalContributorForm(
        form,
        licenceContinuationApplicationDetail
    );

    if (BooleanUtils.isTrue(form.getAddExternalContributors())) {
      var externalContributorsTeam = licenceContinuationExternalContributorService
          .getExternalContributorsTeam(licenceContinuationApplicationDetail);
      return ReverseRouter.redirect(on(TeamManagementController.class)
          .renderExternalContributorsTeamList(externalContributorsTeam.getId(), null));
    }

    return ReverseRouter.redirect(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
        licenceContinuationApplicationDetailId,
        null,
        null
    ));
  }

  private ModelAndView getModelAndView(
      ExternalContributorForm form,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var taskListUrl = ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
        .getTaskList(licenceContinuationApplicationDetail.getId(), null, null));

    var modelAndView = new ModelAndView("lms/licence/application/externalContributor")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("cancelUrl", taskListUrl);

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);
    return modelAndView;
  }
}
