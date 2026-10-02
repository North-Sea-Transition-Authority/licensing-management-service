package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator.LicencePositionHasNoLiveChangeOfType;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceFormService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionLicenseeChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.ListUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.LANDWARD_PRODUCTION, LicenceType.SEAWARD_PRODUCTION, LicenceType.CARBON_STORAGE})
public class LicencePositionLicenseeChangeController {

  private final LicenceFormService licenceFormService;
  private final LicenseeChangeFormValidator licenseeChangeFormValidator;
  private final LicencePositionService licencePositionService;
  private final LicencePositionViewService licencePositionViewService;
  private final LicenseeChangeService licenseeChangeService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  LicencePositionLicenseeChangeController(
      LicenceFormService licenceFormService,
      LicenseeChangeFormValidator licenseeChangeFormValidator,
      LicencePositionService licencePositionService,
      LicencePositionViewService licencePositionViewService,
      LicenseeChangeService licenseeChangeService,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.licenceFormService = licenceFormService;
    this.licenseeChangeFormValidator = licenseeChangeFormValidator;
    this.licencePositionService = licencePositionService;
    this.licencePositionViewService = licencePositionViewService;
    this.licenseeChangeService = licenseeChangeService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @GetMapping("/position/{licencePositionId}/add-licensee-change")
  @LicencePositionHasNoLiveChangeOfType(value = LicenseeOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var licenseeChangeContext = licencePositionViewService.getLicenseeChangeContext(
        correction,
        licencePosition.getId()
    );

    var form = LicencePositionLicenseeChangeUtil.populateLicenseeForm(licenseeChangeContext);

    return getLicenseeChangeModelAndView(
        form,
        executedCancelUrl(correctionId, licencePositionId),
        licenseeChangeContext.previousLicenseeNames()
    );
  }

  @PostMapping("/position/{licencePositionId}/add-licensee-change")
  @LicencePositionHasNoLiveChangeOfType(value = LicenseeOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView submitForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") LicenseeChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var licenseeChangeContext = licencePositionViewService.getLicenseeChangeContext(
        correction,
        licencePosition.getId()
    );

    if (licenseeChangeFormValidator.hasErrors(form, bindingResult, licenseeChangeContext.previousLicenseeIds())) {
      return getLicenseeChangeModelAndView(
          form,
          executedCancelUrl(correctionId, licencePositionId),
          licenseeChangeContext.previousLicenseeNames()
      );
    }

    licenseeChangeService.addLicenseeChangeForExistingLicencePosition(
        licencePosition,
        correction,
        ListUtil.toIntegers(form.getJoiningOrganisationIds()),
        ListUtil.toIntegers(form.getWithdrawingOrganisationIds())
    );

    NotificationBanner.newSuccessBannerWithHeader("Licensee change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-licensee-change")
  public ModelAndView renderForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePositionCorrection = licencePositionCorrectionService.getPositionCorrectionForCorrection(
        licencePositionCorrectionId,
        correction
    );
    var licenseeChangeContext = getAddedPositionLicenseeChangeContext(correction, licencePositionCorrection);

    var form = LicencePositionLicenseeChangeUtil.populateLicenseeForm(licenseeChangeContext);

    return getLicenseeChangeModelAndView(
        form,
        addedCancelUrl(correctionId, licencePositionCorrectionId),
        licenseeChangeContext.previousLicenseeNames()
    );
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/add-licensee-change")
  public ModelAndView submitForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") LicenseeChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePositionCorrection = licencePositionCorrectionService.getPositionCorrectionForCorrection(
        licencePositionCorrectionId,
        correction
    );
    var licenseeChangeContext = getAddedPositionLicenseeChangeContext(correction, licencePositionCorrection);

    if (licenseeChangeFormValidator.hasErrors(
        form,
        bindingResult,
        licenseeChangeContext.previousLicenseeIds()
    )) {
      return getLicenseeChangeModelAndView(
          form,
          addedCancelUrl(correctionId, licencePositionCorrectionId),
          licenseeChangeContext.previousLicenseeNames()
      );
    }

    licenseeChangeService.addLicenseeChangeForAddedLicencePosition(
        licencePositionCorrection,
        ListUtil.toIntegers(form.getJoiningOrganisationIds()),
        ListUtil.toIntegers(form.getWithdrawingOrganisationIds())
    );

    NotificationBanner.newSuccessBannerWithHeader("Licensee change added", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, licencePositionCorrectionId, null));
  }


  private ModelAndView getLicenseeChangeModelAndView(
      LicenseeChangeForm form,
      String cancelUrl,
      List<String> previousLicenseeNames
  ) {
    return new ModelAndView("lms/licence/correction/change/licenseeChange")
        .addObject("previousLicenseeNames", previousLicenseeNames)
        .addObject("pageTitle", "Change licensees")
        .addObject("form", form)
        .addObject("cancelUrl", cancelUrl)
        .addObject("preselectedJoiningOrgUnits",
            licenceFormService.getPreselectedOrganisationUnits(form.getJoiningOrganisationIds()))
        .addObject("preselectedWithdrawingOrgUnits",
            licenceFormService.getPreselectedOrganisationUnits(form.getWithdrawingOrganisationIds()))
        .addObject("joiningOrganisationUnitSearchEndpoint",
            joiningSearchSelectorEndpoint(previousLicenseeNames))
        .addObject("withdrawingOrganisationUnitSearchEndpoint",
            withdrawingSearchSelectorEndpoint(previousLicenseeNames));
  }

  private LicenseeChangeContext getAddedPositionLicenseeChangeContext(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
    return licencePositionViewService.getLicenseeChangeContext(correction, UUID.fromString(payload.licencePositionId()));
  }

  private String joiningSearchSelectorEndpoint(List<String> previousLicenseeNames) {
    if (previousLicenseeNames.isEmpty()) {
      return SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null));
    } else {
      return SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
          .searchOrganisationUnitsWithConstraints(null, null, previousLicenseeNames));
    }
  }

  private String withdrawingSearchSelectorEndpoint(List<String> previousLicenseeNames) {
    if (previousLicenseeNames.isEmpty()) {
      return "";
    } else {
      return SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
          .searchOrganisationUnitsWithConstraints(null, previousLicenseeNames, null));
    }
  }

  private String executedCancelUrl(UUID correctionId, UUID licencePositionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  private String addedCancelUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, licencePositionCorrectionId, null));
  }
}
