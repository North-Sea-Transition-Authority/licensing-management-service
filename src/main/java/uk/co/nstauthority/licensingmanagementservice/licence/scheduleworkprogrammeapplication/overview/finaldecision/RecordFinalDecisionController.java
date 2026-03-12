package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/record-final-decision")
@ScheduleAmendmentApplicationHasStatus(ScheduleWorkProgrammeApplicationStatus.SUBMITTED)
@InvokingUserCanAccessScheduleApplication
@ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint(
    ScheduleWorkProgrammeApplicationActionItem.RECORD_FINAL_DECISION)
public class RecordFinalDecisionController {

  static final String PAGE_TITLE = "Record final decision";

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final RecordFinalDecisionService recordFinalDecisionService;
  private final RecordFinalDecisionFormValidator recordFinalDecisionFormValidator;
  private final FileControllerHelperService fileControllerHelperService;

  public RecordFinalDecisionController(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      RecordFinalDecisionService recordFinalDecisionService,
      RecordFinalDecisionFormValidator recordFinalDecisionFormValidator,
      FileControllerHelperService fileControllerHelperService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.recordFinalDecisionService = recordFinalDecisionService;
    this.recordFinalDecisionFormValidator = recordFinalDecisionFormValidator;
    this.fileControllerHelperService = fileControllerHelperService;
  }

  @GetMapping
  public ModelAndView render(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var form = recordFinalDecisionService.getFormForApplication(applicationDetail);
    return getModelAndView(applicationDetail, form);
  }

  @PostMapping
  ModelAndView save(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      @ModelAttribute("form") RecordFinalDecisionForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (!recordFinalDecisionFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(applicationDetail, form);
    }

    recordFinalDecisionService.recordDecision(applicationDetail, form);

    var applicationReference = applicationDetail.getScheduleWorkProgrammeApplication().getApplicationReference();
    NotificationBanner.newSuccessBanner()
        .withHeadingContent(String.format("Final decision recorded on %s", applicationReference))
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(WorkAreaController.class)
        .getWorkArea(null, null));
  }

  @GetMapping("/files/{fileId}")
  public ResponseEntity<InputStreamResource> downloadFile(
      @PathVariable UUID fileId,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      ServiceUserDetail userDetail
  ) {
    return fileControllerHelperService.download(
        fileId,
        () -> RecordFinalDecisionFileUsage.fromApplication(applicationDetail),
        userDetail
    );
  }

  @PostMapping("/files/delete/{fileId}")
  ResponseEntity<FileDeleteResponse> deleteFile(
      @PathVariable UUID fileId,
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      ServiceUserDetail userDetail
  ) {
    return fileControllerHelperService.delete(
        fileId,
        () -> RecordFinalDecisionFileUsage.fromApplication(applicationDetail),
        userDetail
    );
  }

  private ModelAndView getModelAndView(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      RecordFinalDecisionForm form
  ) {
    var licence = scheduleWorkProgrammeApplicationService
        .getLicenceFromScheduleWorkProgrammeApplicationDetail(applicationDetail);

    var fileUploadAttributes = fileControllerHelperService.fileUploadComponentAttributes(
        form.getFinalDecisionSupportPapers(),
        this.getClass(),
        controller -> controller.downloadFile(null, applicationDetail.getId(), null, null),
        controller -> controller.deleteFile(null, applicationDetail.getId(), null, null),
        "form.finalDecisionSupportPapers"
    );

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/recordFinalDecision")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("caption", licence.getType().getDisplayName())
        .addObject("form", form)
        .addObject("fileUploadAttributes", fileUploadAttributes)
        .addObject("backUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
            .renderOverview(applicationDetail.getId(), null, null)));
  }
}
