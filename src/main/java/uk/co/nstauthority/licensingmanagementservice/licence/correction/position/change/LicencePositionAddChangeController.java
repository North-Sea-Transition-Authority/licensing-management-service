package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Arrays;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee.LicencePositionLicenseeChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.SingleBlockSurrender;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.LicencePositionSubareaChangeStartController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
public class LicencePositionAddChangeController {

  private static final String PAGE_TITLE = "Add change";

  private final AddPositionChangeFormValidator addPositionChangeFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  public LicencePositionAddChangeController(
      AddPositionChangeFormValidator addPositionChangeFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService
  ) {
    this.addPositionChangeFormValidator = addPositionChangeFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
  }

  @GetMapping("/position/{licencePositionId}/add-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return addChangeModelAndView(correction, new AddPositionChangeForm(),
        executedBackUrl(correction, licencePosition));
  }

  @PostMapping("/position/{licencePositionId}/add-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") AddPositionChangeForm form,
      BindingResult bindingResult
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(correction, licencePosition);

    if (addPositionChangeFormValidator.hasErrors(form, bindingResult, correction, positionCorrection)) {
      return addChangeModelAndView(correction, form, executedBackUrl(correction, licencePosition));
    }

    return switch (AddPositionChangeType.valueOf(form.getChangeType())) {
      case ADMINISTRATOR -> ReverseRouter.redirect(on(LicencePositionAdministratorChangeController.class)
          .renderForExecutedPosition(correction, licencePosition));
      case SET_EQUITY -> ReverseRouter.redirect(on(LicencePositionSetEquityController.class)
          .renderForExecutedPosition(correction, licencePosition));
      case TRANSFER_EQUITY -> ReverseRouter.redirect(on(LicencePositionTransferEquityController.class)
          .renderForExecutedPosition(correction, licencePosition));
      case PARTIAL_SURRENDER -> partialSurrenderCorrectionService
          .stageSingleBlockSurrenderForExecutedPosition(correction, licencePosition)
          .map(singleBlockSurrender -> redirectToDefineArea(correction, singleBlockSurrender))
          .orElseGet(() -> ReverseRouter.redirect(on(LicencePositionPartialSurrenderController.class)
              .renderForExecutedPosition(correction, licencePosition)));
      case SUBAREA -> ReverseRouter.redirect(on(LicencePositionSubareaChangeStartController.class)
          .renderForExecutedPosition(correction, licencePosition));
      case LICENSEE -> ReverseRouter.redirect(on(LicencePositionLicenseeChangeController.class)
          .renderForExecutedPosition(correction, licencePosition));
    };
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/add-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    return addChangeModelAndView(correction, new AddPositionChangeForm(),
        addedBackUrl(correction, licencePositionCorrection));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/add-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute("form") AddPositionChangeForm form,
      BindingResult bindingResult
  ) {
    if (addPositionChangeFormValidator.hasErrors(form, bindingResult, correction, licencePositionCorrection)) {
      return addChangeModelAndView(correction, form, addedBackUrl(correction, licencePositionCorrection));
    }

    return switch (AddPositionChangeType.valueOf(form.getChangeType())) {
      case ADMINISTRATOR -> ReverseRouter.redirect(on(LicencePositionAdministratorChangeController.class)
          .renderForAddedPosition(correction, licencePositionCorrection));
      case SET_EQUITY -> ReverseRouter.redirect(on(LicencePositionSetEquityController.class)
          .renderForAddedPosition(correction, licencePositionCorrection));
      case TRANSFER_EQUITY -> ReverseRouter.redirect(on(LicencePositionTransferEquityController.class)
          .renderForAddedPosition(correction, licencePositionCorrection));
      case PARTIAL_SURRENDER -> partialSurrenderCorrectionService
          .stageSingleBlockSurrenderForAddedPosition(licencePositionCorrection)
          .map(singleBlockSurrender -> redirectToDefineArea(correction, singleBlockSurrender))
          .orElseGet(() -> ReverseRouter.redirect(on(LicencePositionPartialSurrenderController.class)
              .renderForAddedPosition(correction, licencePositionCorrection)));
      case SUBAREA -> ReverseRouter.redirect(on(LicencePositionSubareaChangeStartController.class)
          .renderForAddedPosition(correction, licencePositionCorrection));
      case LICENSEE -> ReverseRouter.redirect(on(LicencePositionLicenseeChangeController.class)
          .renderForAddedPosition(correction, licencePositionCorrection));
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

  private ModelAndView redirectToDefineArea(LicenceCorrection correction, SingleBlockSurrender singleBlockSurrender) {
    return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class).renderDefineArea(
        correction,
        singleBlockSurrender.licencePositionCorrection(),
        singleBlockSurrender.block().getId()
    ));
  }

  private String executedBackUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  private String addedBackUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(correction, licencePositionCorrection));
  }

  private Map<String, String> availableChangeTypeOptions(LicenceCorrection correction) {
    var availableChangeTypes = Arrays.stream(AddPositionChangeType.values())
        .filter(changeType -> changeType.isAvailableFor(correction.getLicence().getType()))
        .toList();

    return DisplayableEnumOptionUtil.getDisplayableOptions(availableChangeTypes);
  }
}