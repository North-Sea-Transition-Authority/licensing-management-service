package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/other-requirements")
@ContinuationApplicationHasStatus(value = LicenceContinuationApplicationStatus.DRAFT)
@InvokingUserCanAccessContinuationApplication
public class LicenceContinuationOtherRequirementController {

  public static final String PAGE_TITLE = "Other requirements";
  public final LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService;
  public final LicenceContinuationOtherRequirementValidator licenceContinuationOtherRequirementValidator;
  private final OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService;

  public LicenceContinuationOtherRequirementController(
      LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService,
      LicenceContinuationOtherRequirementValidator licenceContinuationOtherRequirementValidator,
      OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService
  ) {
    this.licenceContinuationOtherRequirementService = licenceContinuationOtherRequirementService;
    this.licenceContinuationOtherRequirementValidator = licenceContinuationOtherRequirementValidator;
    this.otherRequirementsVisibilityResolverService = otherRequirementsVisibilityResolverService;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return getModelAndView(
        licenceContinuationOtherRequirementService.getLicenceContinuationOtherRequirementForm(
            licenceContinuationApplicationDetail),
        licenceContinuationApplicationDetail,
        otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail
        )
    );
  }

  @PostMapping
  ModelAndView submitForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      @ModelAttribute("form") LicenceContinuationOtherRequirementForm form,
      BindingResult bindingResult
  ) {
    var otherRequirementsVisibility = otherRequirementsVisibilityResolverService.resolveVisibility(
        licenceContinuationApplicationDetail
    );

    if (!licenceContinuationOtherRequirementValidator.isValid(form, bindingResult, otherRequirementsVisibility)) {
      return getModelAndView(form, licenceContinuationApplicationDetail, otherRequirementsVisibility);
    }

    licenceContinuationOtherRequirementService.saveLicenceContinuationOtherRequirementForm(
        form,
        licenceContinuationApplicationDetail
    );

    return ReverseRouter.redirect(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
        licenceContinuationApplicationDetailId,
        null,
        null
    ));
  }

  private ModelAndView getModelAndView(
      LicenceContinuationOtherRequirementForm form,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      OtherRequirementsVisibility otherRequirementsVisibility
  ) {
    if (!otherRequirementsVisibility.hasAnyRequirements()) {
      return ReverseRouter.redirect(on(LicenceContinuationApplicationTaskListController.class)
                                        .getTaskList(licenceContinuationApplicationDetail.getId(), null, null));
    }

    return new ModelAndView("lms/licence/continuation/licenceContinuationOtherRequirement")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("otherRequirementsVisibility", otherRequirementsVisibility)
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
                                                        .getTaskList(licenceContinuationApplicationDetail.getId(), null, null)));
  }
}