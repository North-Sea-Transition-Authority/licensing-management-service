package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.energyportalapi.generated.types.Subarea;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/licence-operators")
@ContinuationApplicationHasStatus(value = ApplicationStatus.DRAFT)
@InvokingUserCanAccessContinuationApplication
public class LicenceContinuationLicenceOperatorsController {

  public static final String PAGE_TITLE = "Licence operators";
  
  private final LicenceContinuationLicenceOperatorsService licenceContinuationLicenceOperatorsService;
  private final LicenceContinuationLicenceOperatorsValidator licenceContinuationLicenceOperatorsValidator;

  public LicenceContinuationLicenceOperatorsController(
      LicenceContinuationLicenceOperatorsService licenceContinuationLicenceOperatorsService,
      LicenceContinuationLicenceOperatorsValidator licenceContinuationLicenceOperatorsValidator
  ) {
    this.licenceContinuationLicenceOperatorsService = licenceContinuationLicenceOperatorsService;
    this.licenceContinuationLicenceOperatorsValidator = licenceContinuationLicenceOperatorsValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var subareas = licenceContinuationLicenceOperatorsService
        .getSubareasForApplication(licenceContinuationApplicationDetail);

    var form = licenceContinuationLicenceOperatorsService
        .getLicenceContinuationLicenceOperatorsForm(licenceContinuationApplicationDetail);

    return getModelAndView(form, licenceContinuationApplicationDetail, subareas);

  }

  @PostMapping
  ModelAndView submitForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      @ModelAttribute("form") LicenceContinuationLicenceOperatorsForm form,
      BindingResult bindingResult
  ) {
    var subareas = licenceContinuationLicenceOperatorsService
        .getSubareasForApplication(licenceContinuationApplicationDetail);

    var hasMissingOperators = licenceContinuationLicenceOperatorsService
        .hasMissingOperators(subareas);

    if (!licenceContinuationLicenceOperatorsValidator.isValid(bindingResult, hasMissingOperators)) {
      return getModelAndView(form, licenceContinuationApplicationDetail, subareas);

    }

    licenceContinuationLicenceOperatorsService.saveLicenceContinuationLicenceOperatorsForm(
        form,
        licenceContinuationApplicationDetail
    );

    return ReverseRouter.redirect(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
        licenceContinuationApplicationDetailId, null, null
    ));
  }

  private ModelAndView getModelAndView(
      LicenceContinuationLicenceOperatorsForm form,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      List<Subarea> subareas
  ) {
    var taskListUrl = ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
        .getTaskList(licenceContinuationApplicationDetail.getId(), null, null));

    var modelAndView = new ModelAndView("lms/licence/continuation/licenceOperator/licenceContinuationLicenceOperators")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("hasMissingOperators", licenceContinuationLicenceOperatorsService.hasMissingOperators(subareas))
        .addObject("subareas", subareas)
        .addObject("cancelUrl", taskListUrl);

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);
    return modelAndView;
  }
}