package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
public class LicencePositionAddChangeController {

  private static final String PAGE_TITLE = "Add change";

  private final AddPositionChangeFormValidator addPositionChangeFormValidator;
  private final LicenceService licenceService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;

  public LicencePositionAddChangeController(
      AddPositionChangeFormValidator addPositionChangeFormValidator,
      LicenceService licenceService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService
  ) {
    this.addPositionChangeFormValidator = addPositionChangeFormValidator;
    this.licenceService = licenceService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
  }

  @GetMapping("/position/{licencePositionId}/add-change")
  public ModelAndView renderForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return addChangeModelAndView(correction, new AddPositionChangeForm(),
        executedBackUrl(correctionId, licencePositionId));
  }

  @PostMapping("/position/{licencePositionId}/add-change")
  public ModelAndView submitForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") AddPositionChangeForm form,
      BindingResult bindingResult
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(correction, licencePosition);

    if (addPositionChangeFormValidator.hasErrors(form, bindingResult, correction, positionCorrection)) {
      return addChangeModelAndView(correction, form, executedBackUrl(correctionId, licencePositionId));
    }

    return switch (AddPositionChangeType.valueOf(form.getChangeType())) {
      case ADMINISTRATOR_CHANGE -> ReverseRouter.redirect(on(LicencePositionAdministratorChangeController.class)
          .renderForExecutedPosition(correctionId, licencePositionId, null));
      case SET_EQUITY -> ReverseRouter.redirect(on(LicencePositionSetEquityController.class)
          .renderForExecutedPosition(correctionId, licencePositionId, null));
      case TRANSFER_EQUITY -> ReverseRouter.redirect(on(LicencePositionTransferEquityController.class)
          .renderForExecutedPosition(correctionId, licencePositionId, null));
    };
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-change")
  public ModelAndView renderForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return addChangeModelAndView(correction, new AddPositionChangeForm(),
        addedBackUrl(correctionId, licencePositionCorrectionId));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/add-change")
  public ModelAndView submitForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") AddPositionChangeForm form,
      BindingResult bindingResult
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);

    if (addPositionChangeFormValidator.hasErrors(form, bindingResult, correction, positionCorrection)) {
      return addChangeModelAndView(correction, form, addedBackUrl(correctionId, licencePositionCorrectionId));
    }

    return switch (AddPositionChangeType.valueOf(form.getChangeType())) {
      case ADMINISTRATOR_CHANGE -> ReverseRouter.redirect(on(LicencePositionAdministratorChangeController.class)
          .renderForAddedPosition(correctionId, licencePositionCorrectionId, null));
      case SET_EQUITY -> ReverseRouter.redirect(on(LicencePositionSetEquityController.class)
          .renderForAddedPosition(correctionId, licencePositionCorrectionId, null));
      case TRANSFER_EQUITY -> ReverseRouter.redirect(on(LicencePositionTransferEquityController.class)
          .renderForAddedPosition(correctionId, licencePositionCorrectionId, null));
    };
  }

  private ModelAndView addChangeModelAndView(
      LicenceCorrection correction,
      AddPositionChangeForm form,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/change/addChange")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("changeTypeOptions", availableChangeTypeOptions(correction))
        .addObject("backLinkUrl", backLinkUrl);
  }

  private String executedBackUrl(UUID correctionId, UUID licencePositionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  private String addedBackUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  private Map<String, String> availableChangeTypeOptions(LicenceCorrection correction) {
    var isCarbonStorage = licenceService.isCarbonStorageLicence(correction.getLicence());
    var availableChangeTypes = Arrays.stream(AddPositionChangeType.values())
        .filter(changeType -> changeType == AddPositionChangeType.ADMINISTRATOR_CHANGE || isCarbonStorage)
        .filter(changeType -> changeType != AddPositionChangeType.ADMINISTRATOR_CHANGE || !isCarbonStorage)
        .toList();
    return DisplayableEnumOptionUtil.getDisplayableOptions(availableChangeTypes);
  }
}