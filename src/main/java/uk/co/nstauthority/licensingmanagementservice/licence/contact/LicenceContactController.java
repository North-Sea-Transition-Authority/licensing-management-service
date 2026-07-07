package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

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
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence-contacts")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.LICENSEE_CONTACTS_MANAGEMENT}, teamType = TeamType.ORGANISATION)
})
public class LicenceContactController {

  private final LicenceContactService licenceContactService;
  private final LicenceContactFormValidator licenceContactFormValidator;

  public LicenceContactController(
      LicenceContactService licenceContactService,
      LicenceContactFormValidator licenceContactFormValidator
  ) {
    this.licenceContactService = licenceContactService;
    this.licenceContactFormValidator = licenceContactFormValidator;
  }

  @GetMapping
  public ModelAndView renderManageContacts(ServiceUserDetail user) {
    return new ModelAndView("lms/licence/contact/manageContacts")
        .addObject("pageTitle", "Manage licence contact details")
        .addObject("contactsTableJson", licenceContactService.getContactTableForUser(user));
  }

  @GetMapping("/licence/{licenceId}/responsible-organisation/{responsibleOrganisationId}")
  public ModelAndView renderUpdateContact(
      @PathVariable Integer licenceId,
      @PathVariable Integer responsibleOrganisationId,
      ServiceUserDetail user
  ) {
    var editContext = licenceContactService.getLicenceContactFormView(user, licenceId, responsibleOrganisationId);
    var form = new LicenceContactForm();
    form.setContactEmail(editContext.currentEmail());
    return updateContactModelAndView(editContext, form);
  }

  @PostMapping("/licence/{licenceId}/responsible-organisation/{responsibleOrganisationId}")
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
      return updateContactModelAndView(editContext, form);
    }

    licenceContactService.saveContact(user, licenceId, responsibleOrganisationId, form.getContactEmail());

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Contact email saved")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceContactController.class).renderManageContacts(null));
  }

  private ModelAndView updateContactModelAndView(
      LicenceContactFormView editContext,
      LicenceContactForm form
  ) {
    return new ModelAndView("lms/licence/contact/updateContact")
        .addObject("isUpdate", editContext.isUpdate())
        .addObject("licenceReference", editContext.licenceReference())
        .addObject("form", form)
        .addObject("backLinkUrl", ReverseRouter.route(on(LicenceContactController.class).renderManageContacts(null)));
  }
}
