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
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licence/continuation-application/{licenceContinuationApplicationDetailId}/requirements")
@ContinuationApplicationHasStatus(value = LicenceContinuationApplicationStatus.DRAFT)
@InvokingUserCanAccessContinuationApplication
public class LicenceContinuationWpaRequirementController {

  public static final String PAGE_TITLE = "Work programme";
  public final LicenceContinuationWpaRequirementService licenceContinuationWpaRequirementService;
  public final WorkProgrammeActivityService workProgrammeActivityService;
  public final LicenceContinuationWpaRequirementValidator licenceContinuationWpaRequirementValidator;
  public final LicenceContinuationService licenceContinuationService;

  public LicenceContinuationWpaRequirementController(
      LicenceContinuationWpaRequirementService licenceContinuationWpaRequirementService,
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceContinuationWpaRequirementValidator licenceContinuationWpaRequirementValidator,
      LicenceContinuationService licenceContinuationService
  ) {

    this.licenceContinuationWpaRequirementService = licenceContinuationWpaRequirementService;

    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceContinuationWpaRequirementValidator = licenceContinuationWpaRequirementValidator;
    this.licenceContinuationService = licenceContinuationService;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return getModelAndView(
        licenceContinuationWpaRequirementService.getLicenceContinuationWorkProgrammeActivitiesRequirementForm(
            licenceContinuationApplicationDetail
        ),
        licenceContinuationApplicationDetail
    );
  }

  @PostMapping
  ModelAndView submitForm(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      @ModelAttribute("form") LicenceContinuationWpaRequirementForm form,
      BindingResult bindingResult
  ) {
    if (!licenceContinuationWpaRequirementValidator.isValid(form, bindingResult)) {
      return getModelAndView(form, licenceContinuationApplicationDetail);
    }

    licenceContinuationWpaRequirementService.saveLicenceContinuationWorkProgrammeActivitiesRequirementForm(
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
      LicenceContinuationWpaRequirementForm form,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {

    var scheduleDetail = licenceContinuationService.getScheduleDetailFromApplicationDetail(licenceContinuationApplicationDetail);

    var workProgrammeActivities = workProgrammeActivityService.getCurrentWorkProgrammeActivitiesViews(scheduleDetail);

    return new ModelAndView("lms/licence/continuation/licenceContinuationWpaRequirement")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("workProgrammeActivities", workProgrammeActivities)
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
                licenceContinuationApplicationDetail.getId(), null, null))
        );
  }
}
