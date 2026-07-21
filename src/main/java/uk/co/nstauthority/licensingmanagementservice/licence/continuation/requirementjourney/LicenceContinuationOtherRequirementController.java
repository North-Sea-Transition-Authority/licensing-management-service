package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/other-requirements")
@InvokingUserCanAccessContinuationApplication
public class LicenceContinuationOtherRequirementController {

  public static final String PAGE_TITLE = "Other requirements";
  public final LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService;
  public final LicenceContinuationOtherRequirementValidator licenceContinuationOtherRequirementValidator;
  private final OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService;
  private final FileControllerHelperService fileControllerHelperService;

  public LicenceContinuationOtherRequirementController(
      LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService,
      LicenceContinuationOtherRequirementValidator licenceContinuationOtherRequirementValidator,
      OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService,
      FileControllerHelperService fileControllerHelperService
  ) {
    this.licenceContinuationOtherRequirementService = licenceContinuationOtherRequirementService;
    this.licenceContinuationOtherRequirementValidator = licenceContinuationOtherRequirementValidator;
    this.otherRequirementsVisibilityResolverService = otherRequirementsVisibilityResolverService;
    this.fileControllerHelperService = fileControllerHelperService;
  }

  @GetMapping
  @ContinuationApplicationHasStatus(value = ApplicationStatus.DRAFT)
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
  @ContinuationApplicationHasStatus(value = ApplicationStatus.DRAFT)
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

    var taskListUrl = ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
        .getTaskList(licenceContinuationApplicationDetail.getId(), null, null));

    var fileUploadAttributes = fileControllerHelperService.fileUploadComponentAttributes(
        form.getDocuments(),
        this.getClass(),
        controller -> controller.downloadFile(null, licenceContinuationApplicationDetail.getId(), null, null),
        controller -> controller.deleteFile(null, licenceContinuationApplicationDetail.getId(), null, null)
    );

    var modelAndView = new ModelAndView("lms/licence/continuation/licenceContinuationOtherRequirement")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("otherRequirementsVisibility", otherRequirementsVisibility)
        .addObject("fileUploadAttributes", fileUploadAttributes)
        .addObject("cancelUrl", taskListUrl);

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);
    return modelAndView;
  }

  @GetMapping("/files/{fileId}")
  public ResponseEntity<InputStreamResource> downloadFile(
      @PathVariable UUID fileId,
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail userDetail
  ) {
    return fileControllerHelperService.download(
        fileId,
        () -> LicenceContinuationOtherRequirementFileUsages.fromApplication(licenceContinuationApplicationDetail),
        userDetail
    );
  }

  @PostMapping("/files/delete/{fileId}")
  @ContinuationApplicationHasStatus(value = ApplicationStatus.DRAFT)
  ResponseEntity<FileDeleteResponse> deleteFile(
      @PathVariable UUID fileId,
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail userDetail
  ) {
    return fileControllerHelperService.delete(
        fileId,
        () -> LicenceContinuationOtherRequirementFileUsages.fromApplication(licenceContinuationApplicationDetail),
        userDetail
    );
  }
}