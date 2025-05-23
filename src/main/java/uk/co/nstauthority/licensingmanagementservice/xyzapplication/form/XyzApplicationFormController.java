package uk.co.nstauthority.licensingmanagementservice.xyzapplication.form;

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
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationRestController;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationSupportingDocumentFileUsage;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.tasklist.XyzApplicationTaskListController;

@Controller
@RequestMapping("/application/{applicationId}/form-page")
public class XyzApplicationFormController {

  static final String PAGE_TITLE = "XyzApplication form";

  private final XyzApplicationFormService xyzApplicationFormService;
  private final XyzApplicationFormValidator xyzApplicationFormValidator;
  private final FileControllerHelperService fileControllerHelperService;

  public XyzApplicationFormController(XyzApplicationFormService xyzApplicationFormService,
                                      XyzApplicationFormValidator xyzApplicationFormValidator,
                                      FileControllerHelperService fileControllerHelperService) {
    this.xyzApplicationFormService = xyzApplicationFormService;
    this.xyzApplicationFormValidator = xyzApplicationFormValidator;
    this.fileControllerHelperService = fileControllerHelperService;
  }

  @GetMapping
  public ModelAndView getForm(@PathVariable("applicationId") UUID applicationId,
                              XyzApplication xyzApplication,
                              ServiceUserDetail user) {
    var form = xyzApplicationFormService.getApplicationForm(xyzApplication);
    return getModelAndView(form, applicationId);
  }

  @PostMapping
  public ModelAndView postForm(@PathVariable("applicationId") UUID applicationId,
                               @ModelAttribute("form") XyzApplicationForm form,
                               BindingResult bindingResult,
                               XyzApplication xyzApplication,
                               ServiceUserDetail user) {
    if (xyzApplicationFormValidator.isValid(form, bindingResult)) {
      xyzApplicationFormService.saveApplicationForm(form, xyzApplication);
      return ReverseRouter.redirect(on(XyzApplicationTaskListController.class).getTaskList(applicationId, null, null));
    } else {
      return getModelAndView(form, applicationId);
    }
  }

  private ModelAndView getModelAndView(XyzApplicationForm form, UUID applicationId) {
    var fileUploadAttributes = fileControllerHelperService.fileUploadComponentAttributes(
        form.getDocuments(),
        this.getClass(),
        controller -> controller.downloadFile(null, null, null),
        controller -> controller.deleteFile(null, null, null)
    );

    var taskListUrl = ReverseRouter.route(on(XyzApplicationTaskListController.class).getTaskList(applicationId, null, null));
    var modelAndView = new ModelAndView("lms/application/formPage")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("fileUploadAttributes", fileUploadAttributes)
        .addObject("applicationEndpoint",
            SearchSelectorService.route(on(XyzApplicationRestController.class).searchApplications(null)))
        .addObject("preselectedApplication", xyzApplicationFormService.getPreselectedApplication(form.getSelectedApplication()))
        .addObject("taskListUrl", taskListUrl);

    var breadcrumbs = Breadcrumbs.builder(PAGE_TITLE)
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  @GetMapping("/files/{fileId}")
  ResponseEntity<InputStreamResource> downloadFile(
      @PathVariable UUID fileId,
      XyzApplication xyzApplication,
      ServiceUserDetail userDetail
  ) {
    return fileControllerHelperService.download(
        fileId,
        () -> XyzApplicationSupportingDocumentFileUsage.fromApplication(xyzApplication),
        userDetail
    );
  }

  @PostMapping("/files/delete/{fileId}")
  ResponseEntity<FileDeleteResponse> deleteFile(
      @PathVariable UUID fileId,
      XyzApplication xyzApplication,
      ServiceUserDetail userDetail
  ) {
    return fileControllerHelperService.delete(
        fileId,
        () -> XyzApplicationSupportingDocumentFileUsage.fromApplication(xyzApplication),
        userDetail
    );
  }

}
