package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

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
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Controller
@RequestMapping("licence/{licenceId}/continuation-application/licensee-information")
@InvokingUserCanStartApplication
public class LicenceContinuationLicenseeInformationController {
  public static final String PAGE_TITLE = "Licensee information";

  private final LicenceContinuationLicenseeInformationFormValidator licenceContinuationLicenseeInformationFormValidator;
  private final LicenceContinuationService licenceContinuationService;
  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final TeamManagementService teamManagementService;

  public LicenceContinuationLicenseeInformationController(
      LicenceContinuationLicenseeInformationFormValidator licenceContinuationLicenseeInformationFormValidator,
      LicenceContinuationService licenceContinuationService,
      LicenceService licenceService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      TeamManagementService teamManagementService
  ) {
    this.licenceContinuationLicenseeInformationFormValidator = licenceContinuationLicenseeInformationFormValidator;
    this.licenceContinuationService = licenceContinuationService;
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.teamManagementService = teamManagementService;
  }

  @GetMapping
  ModelAndView renderConfirmLicenseePermission(
      @PathVariable Integer licenceId,
      Licence licence,
      ServiceUserDetail user
  ) {
    return getLicenseePermissionConfirmationModelAndView(new LicenceContinuationLicenseeInformationForm(), licence, user);
  }

  @PostMapping
  ModelAndView submitLicenseePermissionConfirmation(
      @PathVariable Integer licenceId,
      Licence licence,
      @ModelAttribute("form") LicenceContinuationLicenseeInformationForm form,
      BindingResult bindingResult,
      ServiceUserDetail user
  ) {
    if (!licenceContinuationLicenseeInformationFormValidator.isValid(bindingResult)) {
      return getLicenseePermissionConfirmationModelAndView(form, licence, user);
    }

    var responsibleOrganisationUnitId = form.getResponsibleOrganisationUnitId();
    var applicationDetail = licenceContinuationService.createNewLicenceContinuationApplication(
        licence,
        responsibleOrganisationUnitId
    );

    createContinuationExternalTeam(applicationDetail);

    return ReverseRouter.redirect(on(LicenceContinuationApplicationTaskListController.class)
                                      .getTaskList(applicationDetail.getId(), null, null));
  }

  private void createContinuationExternalTeam(LicenceContinuationApplicationDetail applicationDetail) {
    var scopeRef = TeamScopeReference.from(
        applicationDetail.getId().toString(),
        ApplicationType.CONTINUATION_APPLICATION.name()
    );

    teamManagementService.createScopedTeam(
        TeamType.EXTERNAL_CONTRIBUTORS.getDisplayName(),
        TeamType.EXTERNAL_CONTRIBUTORS,
        scopeRef
    );
  }

  private ModelAndView getLicenseePermissionConfirmationModelAndView(
      LicenceContinuationLicenseeInformationForm selectLicenceTypeForm,
      Licence licence,
      ServiceUserDetail user
  ) {
    var caption = licenceService.getLicencePageCaption(licence);

    return new ModelAndView("lms/licence/continuation/licenseeInformationContinuation")
        .addObject("form", selectLicenceTypeForm)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", caption)
        .addObject("responsibleOrgUnitOptions", licenceResponsibleOrganisationService
            .getResponsibleOrgUnitOptionsWithValidRoles(licence, user))
        .addObject("backUrl", ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class)
            .render())
        );
  }
}