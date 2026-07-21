package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.steward;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/allocate-steward")
@ScheduleAmendmentApplicationHasStatus(ApplicationStatus.SUBMITTED)
@InvokingUserCanAccessScheduleApplication
@ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint(
    ScheduleWorkProgrammeApplicationActionItem.ALLOCATE_STEWARD)
public class AllocateStewardController {

  static final String PAGE_TITLE = "Who is the steward for this application?";

  private final AllocateStewardService allocateStewardService;
  private final AllocateStewardValidator allocateStewardValidator;

  public AllocateStewardController(
      AllocateStewardService allocateStewardService,
      AllocateStewardValidator allocateStewardValidator
  ) {
    this.allocateStewardService = allocateStewardService;
    this.allocateStewardValidator = allocateStewardValidator;
  }

  @GetMapping
  public ModelAndView render(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var stewardOptions = allocateStewardService.getStewardOptions();
    var form = allocateStewardService.getFormForApplication(
        applicationDetail.getScheduleWorkProgrammeApplication());
    return getModelAndView(applicationDetail, form, stewardOptions);
  }

  @PostMapping
  ModelAndView save(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      @ModelAttribute("form") AllocateStewardForm form,
      BindingResult bindingResult
  ) {
    var stewardOptions = allocateStewardService.getStewardOptions();
    if (!allocateStewardValidator.isValid(form, bindingResult, stewardOptions)) {
      return getModelAndView(applicationDetail, form, stewardOptions);
    }

    allocateStewardService.saveSteward(
        applicationDetail.getScheduleWorkProgrammeApplication(),
        Long.parseLong(form.getStewardWuaId())
    );

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationOverviewController.class)
        .renderOverview(applicationDetail.getId(), null, null));
  }

  private ModelAndView getModelAndView(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      AllocateStewardForm form,
      Map<String, String> stewardOptions
  ) {
    var licence = applicationDetail.getLicence();

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/allocateSteward")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("stewardOptions", stewardOptions)
        .addObject("caption", licence.getType().getDisplayName())
        .addObject("backUrl", ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
            .renderOverview(applicationDetail.getId(), null, null)));
  }
}
