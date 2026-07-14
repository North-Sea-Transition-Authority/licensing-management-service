package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence-contacts")
public class LicenceContactController {

  private final LicenceContactService licenceContactService;
  private final LicenceContactFormValidator licenceContactFormValidator;
  private final TeamQueryService teamQueryService;
  private final RegulatorRoleService regulatorRoleService;

  public LicenceContactController(
      LicenceContactService licenceContactService,
      LicenceContactFormValidator licenceContactFormValidator,
      TeamQueryService teamQueryService,
      RegulatorRoleService regulatorRoleService
  ) {
    this.licenceContactService = licenceContactService;
    this.licenceContactFormValidator = licenceContactFormValidator;
    this.teamQueryService = teamQueryService;
    this.regulatorRoleService = regulatorRoleService;
  }

  @GetMapping
  public ModelAndView renderManageContacts(ServiceUserDetail user) {
    if (regulatorRoleService.isRegulator(user)) {
      return contactsModelAndView("Licence contact details", licenceContactService.getRegulatorContactsTable());
    }

    var canManage = teamQueryService.userHasRoleInTeamType(
        user.wuaId(), TeamType.ORGANISATION, Set.of(Role.LICENSEE_CONTACTS_MANAGER));
    var pageTitle = canManage ? "Manage licence contact details" : "Licence contact details";
    return contactsModelAndView(pageTitle, licenceContactService.getIndustryContactsTable(user, canManage));
  }

  private ModelAndView contactsModelAndView(String pageTitle, String contactsTableJson) {
    return new ModelAndView("lms/licence/contact/manageContacts")
        .addObject("pageTitle", pageTitle)
        .addObject("contactsTableJson", contactsTableJson);
  }

  @GetMapping("/licence/{licenceId}/responsible-organisation/{responsibleOrganisationId}")
  @HasRolesInTeamType(@RolesAndTeamType(roles = {Role.LICENSEE_CONTACTS_MANAGER}, teamType = TeamType.ORGANISATION))
  public ModelAndView renderUpdateContact(
      @PathVariable Integer licenceId,
      @PathVariable Integer responsibleOrganisationId,
      ServiceUserDetail user
  ) {
    var editContext = licenceContactService.getLicenceContactFormView(user, licenceId, responsibleOrganisationId);
    var form = new LicenceContactForm();
    form.setContactEmail(editContext.currentEmail());
    return updateContactModelAndView(licenceId, responsibleOrganisationId, user, editContext, form);
  }

  @PostMapping("/licence/{licenceId}/responsible-organisation/{responsibleOrganisationId}")
  @HasRolesInTeamType(@RolesAndTeamType(roles = {Role.LICENSEE_CONTACTS_MANAGER}, teamType = TeamType.ORGANISATION))
  public ModelAndView saveContact(
      @PathVariable Integer licenceId,
      @PathVariable Integer responsibleOrganisationId,
      @ModelAttribute("form") LicenceContactForm form,
      BindingResult bindingResult,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    licenceContactFormValidator.isValid(form, bindingResult);

    if (bindingResult.hasErrors()) {
      var editContext = licenceContactService.getLicenceContactFormView(user, licenceId, responsibleOrganisationId);
      return updateContactModelAndView(licenceId, responsibleOrganisationId, user, editContext, form);
    }

    var licenceIds = new ArrayList<Integer>();
    licenceIds.add(licenceId);
    licenceIds.addAll(form.getBulkUpdateLicenceIds());

    licenceContactService.applyContactToLicences(user, responsibleOrganisationId, form.getContactEmail(), licenceIds);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Contact email saved")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceContactController.class).renderManageContacts(null));
  }

  private ModelAndView updateContactModelAndView(
      Integer licenceId,
      Integer organisationId,
      ServiceUserDetail user,
      LicenceContactFormView editContext,
      LicenceContactForm form
  ) {
    var otherLicences = licenceContactService.getOtherLicencesHeldByLicensee(user, licenceId, organisationId);

    return new ModelAndView("lms/licence/contact/updateContact")
        .addObject("isUpdate", editContext.isUpdate())
        .addObject("licenceReference", editContext.licenceReference())
        .addObject("form", form)
        .addObject("otherLicences", otherLicences)
        .addObject("licenseeName", otherLicences.isEmpty() ? null : otherLicences.getFirst().licenseeName())
        .addObject("backLinkUrl", ReverseRouter.route(on(LicenceContactController.class).renderManageContacts(null)));
  }
}
