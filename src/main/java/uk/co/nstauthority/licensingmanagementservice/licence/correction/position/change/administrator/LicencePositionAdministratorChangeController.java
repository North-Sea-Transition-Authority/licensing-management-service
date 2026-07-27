package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.ValidLicencePositionAdministratorChange;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
public class LicencePositionAdministratorChangeController {

  private static final String PAGE_TITLE = "Change licence administrator";

  private final AdministratorChangeFormValidator administratorChangeFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionAdministratorChangeController(
      AdministratorChangeFormValidator administratorChangeFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.administratorChangeFormValidator = administratorChangeFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  @GetMapping("/position/{licencePositionId}/add-administrator-change")
  public ModelAndView renderForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var currentAdministratorId =
        licencePositionService.getCurrentAdministratorIdForCorrection(correction, licencePosition.getId());

    return getAdministratorChangeModelAndView(
        new AdministratorChangeForm(),
        executedBackUrl(correctionId, licencePositionId),
        previousAdministratorName(currentAdministratorId)
    );
  }

  @PostMapping("/position/{licencePositionId}/add-administrator-change")
  public ModelAndView submitForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") AdministratorChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var currentAdministratorId =
        licencePositionService.getCurrentAdministratorIdForCorrection(correction, licencePosition.getId());

    if (administratorChangeFormValidator.hasErrors(form, bindingResult, currentAdministratorId)) {
      return getAdministratorChangeModelAndView(
          form,
          executedBackUrl(correctionId, licencePositionId),
          previousAdministratorName(currentAdministratorId)
      );
    }

    licencePositionCorrectionService.addAdministratorChangeForExistingLicencePosition(
        licencePosition, correction, Integer.parseInt(form.getAdminId().getInputValue()));

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-administrator-change")
  public ModelAndView renderForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePositionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var currentAdministratorId = getAddedPositionCurrentAdministratorId(licencePositionCorrection);

    return getAdministratorChangeModelAndView(
        new AdministratorChangeForm(),
        addedBackUrl(correctionId, licencePositionCorrectionId),
        previousAdministratorName(currentAdministratorId)
    );
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/add-administrator-change")
  public ModelAndView submitForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") AdministratorChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePositionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var currentAdministratorId = getAddedPositionCurrentAdministratorId(licencePositionCorrection);

    if (administratorChangeFormValidator.hasErrors(form, bindingResult, currentAdministratorId)) {
      return getAdministratorChangeModelAndView(
          form,
          addedBackUrl(correctionId, licencePositionCorrectionId),
          previousAdministratorName(currentAdministratorId)
      );
    }

    licencePositionCorrectionService.addAdministratorChangeForAddedLicencePosition(
        licencePositionCorrection, Integer.parseInt(form.getAdminId().getInputValue()));

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/correct-administrator-change")
  @LicencePositionChangeBelongsToPosition
  @ValidLicencePositionAdministratorChange
  public ModelAndView renderForCorrectingChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var currentAdministratorId = licencePositionService.getCurrentAdministratorIdForCorrection(correction, licencePositionId);
    var form = new AdministratorChangeForm();
    if (currentAdministratorId != null) {
      form.getAdminId().setInputValue(String.valueOf(currentAdministratorId));
    }
    var previousAdministratorId = licencePositionService.getPreviousAdministratorIdForCorrection(correction, licencePositionId);
    return getAdministratorChangeModelAndView(form, executedBackUrl(correctionId, licencePositionId),
        // Correcting an existing change, so the administrator being replaced is the previous one this change
        // superseded, not the current administrator the change installed.
        previousAdministratorName(previousAdministratorId));
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/correct-administrator-change")
  @LicencePositionChangeBelongsToPosition
  @ValidLicencePositionAdministratorChange
  public ModelAndView submitForCorrectingChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") AdministratorChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var currentAdministratorId = licencePositionService.getCurrentAdministratorIdForCorrection(correction, licencePositionId);
    var previousAdministratorId = licencePositionService.getPreviousAdministratorIdForCorrection(correction, licencePositionId);

    if (administratorChangeFormValidator.hasErrors(form, bindingResult, currentAdministratorId, previousAdministratorId)) {
      return getAdministratorChangeModelAndView(form, executedBackUrl(correctionId, licencePositionId),
          // Correcting an existing change, so the administrator being replaced is the previous one this change
          // superseded, not the current administrator the change installed.
          previousAdministratorName(previousAdministratorId));
    }

    licencePositionCorrectionService.correctExistingAdministratorChange(
        licencePosition, correction, changeId, Integer.parseInt(form.getAdminId().getInputValue()));

    NotificationBanner.newSuccessBannerWithHeader("Licence administrator change corrected", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  private Integer getAddedPositionCurrentAdministratorId(
      LicencePositionCorrection licencePositionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
    return licencePositionService.getCurrentAdministratorIdForCorrection(
        licencePositionCorrection.getLicenceCorrection(), UUID.fromString(payload.licencePositionId()));
  }

  private String executedBackUrl(UUID correctionId, UUID licencePositionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  private String addedBackUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  private String previousAdministratorName(Integer administratorId) {
    if (administratorId == null) {
      return "";
    }
    return organisationUnitQueryService.getOrganisationUnitNameById(administratorId).orElse("");
  }

  private Map<String, String> preselectedAdministrator(AdministratorChangeForm form) {
    var inputValue = form.getAdminId().getInputValue();
    if (StringUtils.isBlank(inputValue)) {
      return Map.of();
    }
    try {
      var administratorId = Integer.parseInt(inputValue);
      return organisationUnitQueryService.getOrganisationUnitNameById(administratorId)
          .map(name -> Map.of(inputValue, name))
          .orElse(Map.of());
    } catch (NumberFormatException ex) {
      return Map.of();
    }
  }

  private ModelAndView getAdministratorChangeModelAndView(
      AdministratorChangeForm form,
      String backLinkUrl,
      String previousAdministratorName
  ) {
    return new ModelAndView("lms/licence/correction/change/administratorChange")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("backLinkUrl", backLinkUrl)
        .addObject("previousLicenceAdministratorName", previousAdministratorName)
        .addObject("preselectedAdministrator", preselectedAdministrator(form))
        .addObject("organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)));
  }
}
