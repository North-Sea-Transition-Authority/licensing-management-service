package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.RequestPurposeChoiceMustBeApplicable;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/" +
    "work-programme-amendment/summary")
@ScheduleAmendmentApplicationHasStatus(value = ScheduleWorkProgrammeApplicationStatus.DRAFT)
@InvokingUserCanAccessScheduleApplication
@RequestPurposeChoiceMustBeApplicable
public class LicenceWorkProgrammeAmendmentSummaryController {
  private static final String PAGE_TITLE = "Work programme amendments";
  private final LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;
  private final LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;
  private final LicenceWorkProgrammeAmendmentSummaryFormValidator licenceWorkProgrammeAmendmentSummaryFormValidator;

  public LicenceWorkProgrammeAmendmentSummaryController(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository,
      LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService,
      LicenceWorkProgrammeAmendmentSummaryFormValidator licenceWorkProgrammeAmendmentSummaryFormValidator) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
    this.licenceWorkProgrammeAmendmentSummaryService = licenceWorkProgrammeAmendmentSummaryService;
    this.licenceWorkProgrammeAmendmentSummaryFormValidator = licenceWorkProgrammeAmendmentSummaryFormValidator;
  }

  @GetMapping
  public ModelAndView renderForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var workProgrammeAmendmentRequests = licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail
    );

    if (workProgrammeAmendmentRequests.isEmpty()) {
      return ReverseRouter.redirect(
          on(SelectLicenceWorkAmendmentController.class)
              .renderForm(scheduleWorkProgrammeApplicationDetailId, null));
    }

    var amendmentSummaryForm = licenceWorkProgrammeAmendmentSummaryService.getLicenceWorkProgrammeAmendmentSummaryForm(
        scheduleWorkProgrammeApplicationDetail
    );

    return getModelAndView(
        amendmentSummaryForm,
        scheduleWorkProgrammeApplicationDetail
    );
  }

  @PostMapping
  ModelAndView submitForm(
      @PathVariable UUID scheduleWorkProgrammeApplicationDetailId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      @ModelAttribute("form") LicenceWorkProgrammeAmendmentSummaryForm form,
      BindingResult bindingResult
  ) {

    if (!licenceWorkProgrammeAmendmentSummaryFormValidator.isValid(bindingResult)) {
      return getModelAndView(form, scheduleWorkProgrammeApplicationDetail);
    }

    licenceWorkProgrammeAmendmentSummaryService.saveWorkProgrammeAmendmentSummaryForm(
        form,
        scheduleWorkProgrammeApplicationDetail
    );

    LicenceWorkProgrammeAmendmentSummaryOptions option = form.getLicenceWorkProgrammeAmendmentSummaryOptions();

    if (option == LicenceWorkProgrammeAmendmentSummaryOptions.YES_NOW) {
      return ReverseRouter.redirect(on(SelectLicenceWorkAmendmentController.class).renderForm(
          scheduleWorkProgrammeApplicationDetailId,
          scheduleWorkProgrammeApplicationDetail
      ));
    }

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(
        scheduleWorkProgrammeApplicationDetailId,
        scheduleWorkProgrammeApplicationDetail,
        null
    ));
  }

  private ModelAndView getModelAndView(
      LicenceWorkProgrammeAmendmentSummaryForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var workProgrammeAmendmentSummaryViews = licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentSummaryViews(
        scheduleWorkProgrammeApplicationDetail
    );

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendmentSummary")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("licenceWorkProgrammeAmendmentSummaryOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceWorkProgrammeAmendmentSummaryOptions.class))
        .addObject("licenceWorkProgrammeAmendments", workProgrammeAmendmentSummaryViews)
        .addObject("cancelUrl", ReverseRouter.route(
        on(ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null)));
  }
}