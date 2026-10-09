package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator.LicencePositionHasNoLiveChangeOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceFormService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
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
  private final LicencePositionViewService licencePositionViewService;
  private final LicenseeChangeService licenseeChangeService;

  LicencePositionLicenseeChangeController(
      LicenceFormService licenceFormService,
      LicenseeChangeFormValidator licenseeChangeFormValidator,
      LicencePositionViewService licencePositionViewService,
      LicenseeChangeService licenseeChangeService
  ) {
    this.licenceFormService = licenceFormService;
    this.licenseeChangeFormValidator = licenseeChangeFormValidator;
    this.licencePositionViewService = licencePositionViewService;
    this.licenseeChangeService = licenseeChangeService;
  }

  @GetMapping("/position/{licencePositionId}/add-licensee-change")
  @LicencePositionHasNoLiveChangeOfType(value = LicenseeOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return renderForExistingPosition(correction, licencePosition);
  }

  @PostMapping("/position/{licencePositionId}/add-licensee-change")
  @LicencePositionHasNoLiveChangeOfType(value = LicenseeOperation.class)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") LicenseeChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licenseeChangeContext = licencePositionViewService.getLicenseeChangeContext(
        correction,
        licencePosition.getId()
    );

    if (licenseeChangeFormValidator.hasErrors(form, bindingResult, licenseeChangeContext.previousLicenseeIds())) {
      return getLicenseeChangeModelAndView(
          form,
          executedCancelUrl(correction, licencePosition),
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
        .renderLicencePosition(correction, licencePosition));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-licensee-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var licenseeChangeContext = getAddedPositionLicenseeChangeContext(correction, licencePositionCorrection);

    var form = LicencePositionLicenseeChangeUtil.populateLicenseeForm(licenseeChangeContext);

    return getLicenseeChangeModelAndView(
        form,
        addedCancelUrl(correction, licencePositionCorrection),
        licenseeChangeContext.previousLicenseeNames()
    );
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/add-licensee-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute("form") LicenseeChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licenseeChangeContext = getAddedPositionLicenseeChangeContext(correction, licencePositionCorrection);

    if (licenseeChangeFormValidator.hasErrors(
        form,
        bindingResult,
        licenseeChangeContext.previousLicenseeIds()
    )) {
      return getLicenseeChangeModelAndView(
          form,
          addedCancelUrl(correction, licencePositionCorrection),
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
        .renderAddedPosition(correction, licencePositionCorrection));
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/correct-licensee-change")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeIsOfType(LicenseeOperation.class)
  public ModelAndView renderForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change
  ) {
    return renderForExistingPosition(correction, licencePosition);
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/correct-licensee-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(LicenseeOperation.class)
  public ModelAndView submitForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      @ModelAttribute("form") LicenseeChangeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licenseeChangeContext = licencePositionViewService.getLicenseeChangeContext(
        correction,
        licencePosition.getId()
    );

    if (licenseeChangeFormValidator.hasErrors(form, bindingResult, licenseeChangeContext.previousLicenseeIds())) {
      return getLicenseeChangeModelAndView(
          form,
          executedCancelUrl(correction, licencePosition),
          licenseeChangeContext.previousLicenseeNames()
      );
    }

    licenseeChangeService.correctExistingLicenseeChange(
        licencePosition,
        correction,
        ListUtil.toIntegers(form.getJoiningOrganisationIds()),
        ListUtil.toIntegers(form.getWithdrawingOrganisationIds()),
        change.getId().toString()
    );

    NotificationBanner.newSuccessBannerWithHeader("Licensee change corrected", redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
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

  private ModelAndView renderForExistingPosition(LicenceCorrection correction, LicencePosition position) {
    var licenseeChangeContext = licencePositionViewService.getLicenseeChangeContext(
        correction,
        position.getId()
    );

    var form = LicencePositionLicenseeChangeUtil.populateLicenseeForm(licenseeChangeContext);
    return getLicenseeChangeModelAndView(
        form,
        executedCancelUrl(correction, position),
        licenseeChangeContext.previousLicenseeNames()
    );
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

  private String executedCancelUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  private String addedCancelUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection));
  }
}
