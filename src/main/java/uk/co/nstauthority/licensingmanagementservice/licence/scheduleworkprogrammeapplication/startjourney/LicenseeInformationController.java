package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licence/{licenceId}/schedule-work-programme-application/{licenceTypeSlug}/licensee-information")
@InvokingUserCanStartApplication
public class LicenseeInformationController {
  public static final String PAGE_TITLE = "Licensee information";

  private final LicenseeInformationFormValidator licenseeInformationFormValidator;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

  public LicenseeInformationController(
      LicenseeInformationFormValidator licenseeInformationFormValidator,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceService licenceService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      SwpApplicationRequestPurposeService swpApplicationRequestPurposeService
  ) {
    this.licenseeInformationFormValidator = licenseeInformationFormValidator;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.swpApplicationRequestPurposeService = swpApplicationRequestPurposeService;
  }

  @GetMapping
  ModelAndView renderConfirmLicenseePermission(@PathVariable String licenceTypeSlug,
                                               @PathVariable Integer licenceId,
                                               Licence licence,
                                               ServiceUserDetail user) {
    return getLicenseePermissionConfirmationModelAndView(new LicenseeInformationForm(), licenceTypeSlug, licence, user);
  }

  @PostMapping
  ModelAndView submitLicenseePermissionConfirmation(
      @PathVariable String licenceTypeSlug,
      @PathVariable Integer licenceId,
      Licence licence,
      @ModelAttribute("form") LicenseeInformationForm form,
      BindingResult bindingResult,
      ServiceUserDetail user
  ) {

    if (!licenseeInformationFormValidator.isValid(form, bindingResult)) {
      return getLicenseePermissionConfirmationModelAndView(form, licenceTypeSlug, licence, user);
    }

    var applicationDetail = scheduleWorkProgrammeApplicationService
        .createNewScheduleWorkProgrammeApplicationForLicence(licence, form);

    swpApplicationRequestPurposeService.applyDefaultRequestPurposeIfNotApplicable(applicationDetail);

    return ReverseRouter.redirect(on(ScheduleWorkProgrammeApplicationTaskListController.class)
        .getTaskList(applicationDetail.getId(), null, null));
  }

  private ModelAndView getLicenseePermissionConfirmationModelAndView(
      LicenseeInformationForm selectLicenceTypeForm,
      String licenceTypeSlug,
      Licence licence,
      ServiceUserDetail user) {
    var licenceType = LicenceType.getFromSlugOrThrow(licenceTypeSlug);
    var caption = licenceService.getLicencePageCaption(licence);

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/licenseeInformation")
        .addObject("form", selectLicenceTypeForm)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", caption)
        .addObject("responsibleOrgUnitOptions", licenceResponsibleOrganisationService
            .getResponsibleOrgUnitOptionsWithValidRoles(licence, user))
        .addObject("backUrl", ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class)
            .renderSelectLicenceForScheduleWorkProgrammeApplication(licenceType.getUrlSlug()))
        );
  }
}