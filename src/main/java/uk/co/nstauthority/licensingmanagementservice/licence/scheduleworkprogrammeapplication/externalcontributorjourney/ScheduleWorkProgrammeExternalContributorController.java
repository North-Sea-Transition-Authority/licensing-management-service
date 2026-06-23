package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementController;

@Controller
@RequestMapping("licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/external-contributors")
@ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
public class ScheduleWorkProgrammeExternalContributorController {

  public static final String PAGE_TITLE = "External contributors";

  private final ScheduleWorkProgrammeExternalContributorService scheduleWorkProgrammeExternalContributorService;
  private final ExternalContributorFormValidator externalContributorFormValidator;

  public ScheduleWorkProgrammeExternalContributorController(
      ScheduleWorkProgrammeExternalContributorService scheduleWorkProgrammeExternalContributorService,
      ExternalContributorFormValidator externalContributorFormValidator
  ) {
    this.scheduleWorkProgrammeExternalContributorService = scheduleWorkProgrammeExternalContributorService;
    this.externalContributorFormValidator = externalContributorFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getModelAndView(
        scheduleWorkProgrammeExternalContributorService.getExternalContributorForm(scheduleWorkProgrammeApplicationDetail),
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping
  public ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") ExternalContributorForm form,
      BindingResult bindingResult
  ) {
    if (!externalContributorFormValidator.isValid(bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    scheduleWorkProgrammeExternalContributorService.saveExternalContributorForm(
        form,
        scheduleWorkProgrammeApplicationDetail
    );

    if (BooleanUtils.isTrue(form.getAddExternalContributors())) {
      var externalContributorsTeam = scheduleWorkProgrammeExternalContributorService
          .getExternalContributorsTeam(scheduleWorkProgrammeApplicationDetail);
      return ReverseRouter.redirect(on(TeamManagementController.class)
          .renderExternalContributorsTeamList(externalContributorsTeam.getId(), null));
    }

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
        scheduleWorkProgrammeApplicationDetailId,
        null,
        null
    ));
  }

  private ModelAndView getModelAndView(
      ExternalContributorForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return new ModelAndView("lms/licence/application/externalContributor")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("cancelUrl", ReverseRouter.route(
            on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
                scheduleWorkProgrammeApplicationDetail.getId(), null, null
            )));
  }
}
