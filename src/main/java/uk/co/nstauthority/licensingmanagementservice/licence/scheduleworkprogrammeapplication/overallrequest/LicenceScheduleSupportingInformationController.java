package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/supporting-information")
@InvokingUserCanAccessScheduleApplication
public class LicenceScheduleSupportingInformationController {

  public static final String PAGE_TITLE = "Supporting information";
  private final LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService;
  private final LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;
  private final LicenceScheduleSupportingInformationFormValidator licenceScheduleSupportingInformationFormValidator;
  private final FileControllerHelperService fileControllerHelperService;

  public LicenceScheduleSupportingInformationController(

      LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService,
      LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService,
      LicenceScheduleSupportingInformationFormValidator licenceScheduleSupportingInformationFormValidator,
      FileControllerHelperService fileControllerHelperService
  ) {
    this.licenceScheduleSupportingInformationHelperService = licenceScheduleSupportingInformationHelperService;
    this.licenceScheduleSupportingInformationService = licenceScheduleSupportingInformationService;
    this.licenceScheduleSupportingInformationFormValidator = licenceScheduleSupportingInformationFormValidator;
    this.fileControllerHelperService = fileControllerHelperService;
  }

  @GetMapping
  @ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(
        licenceScheduleSupportingInformationService.getLicenceScheduleRequestForm(scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping
  @ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
  ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") LicenceScheduleSupportingInformationForm form,
      BindingResult bindingResult
  ) {

    if (!licenceScheduleSupportingInformationFormValidator.isValid(bindingResult, scheduleWorkProgrammeApplicationDetail)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    licenceScheduleSupportingInformationService.saveRequestForm(form, scheduleWorkProgrammeApplicationDetail);

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(scheduleWorkProgrammeApplicationDetailId, scheduleWorkProgrammeApplicationDetail, null));
  }

  private ModelAndView getModelAndView(
      LicenceScheduleSupportingInformationForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var fileUploadAttributes = fileControllerHelperService.fileUploadComponentAttributes(
        form.getDocuments(),
        this.getClass(),
        controller -> controller.downloadFile(null, scheduleWorkProgrammeApplicationDetail.getId(), null, null),
        controller -> controller.deleteFile(null, scheduleWorkProgrammeApplicationDetail.getId(), null, null)
    );

    var modelAndView = new ModelAndView(
        "lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceSupportingInformationRequest");
    modelAndView.addObject("pageTitle", PAGE_TITLE)
                .addObject("form", form)
                .addObject("fileUploadAttributes", fileUploadAttributes)
                .addObject("isExtension", licenceScheduleSupportingInformationHelperService.isExtensionOrAmendment(
                    scheduleWorkProgrammeApplicationDetail))
        .addObject("cancelUrl", ReverseRouter.route(
            on(ScheduleWorkProgrammeApplicationTaskListController.class)
                .getTaskList(
                    scheduleWorkProgrammeApplicationDetail.getId(),
                    null,
                    null
                ))
        );
    return modelAndView;
  }

  @GetMapping("/files/{fileId}")
  public ResponseEntity<InputStreamResource> downloadFile(
      @PathVariable UUID fileId,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail userDetail
  ) {
    return fileControllerHelperService.download(
        fileId,
        () -> LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail),
        userDetail
    );
  }

  @PostMapping("/files/delete/{fileId}")
  @ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
  ResponseEntity<FileDeleteResponse> deleteFile(
      @PathVariable UUID fileId,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail userDetail
  ) {
    return fileControllerHelperService.delete(
        fileId,
        () -> LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail),
        userDetail
    );
  }
}