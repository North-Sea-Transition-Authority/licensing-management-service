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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.SingleBlockSurrender;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.LicencePositionSubareaChangeStartController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity.LicencePositionSubareaSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity.SubareaEquityOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@InvokingUserCanViewCorrection
public class LicencePositionAddChangeController {

  private static final String PAGE_TITLE = "Add change";

  private final AddPositionChangeFormValidator addPositionChangeFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final SubareaEquityOptionsService subareaEquityOptionsService;

  public LicencePositionAddChangeController(
      AddPositionChangeFormValidator addPositionChangeFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      SubareaEquityOptionsService subareaEquityOptionsService
  ) {
    this.addPositionChangeFormValidator = addPositionChangeFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.subareaEquityOptionsService = subareaEquityOptionsService;
  }

  @GetMapping("/position/{licencePositionId}/add-change")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderForPosition(CorrectionPosition correctionPosition) {
    return addChangeModelAndView(
        correctionPosition.correction(),
        new AddPositionChangeForm(),
        subareaEquityOptionsService.getSubareaOptions(correctionPosition),
        LicencePositionCorrectionRouteUtil.getPositionPageUrl(correctionPosition)
    );
  }

  @PostMapping("/position/{licencePositionId}/add-change")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView submitForPosition(
      CorrectionPosition correctionPosition,
      @ModelAttribute("form") AddPositionChangeForm form,
      BindingResult bindingResult
  ) {
    var correction = correctionPosition.correction();
    var positionCorrection = licencePositionCorrectionService.getOrBuildPositionCorrection(correctionPosition);
    var subareaOptions = subareaEquityOptionsService.getSubareaOptions(correctionPosition);

    if (addPositionChangeFormValidator.hasErrors(
        form,
        bindingResult,
        correction,
        positionCorrection,
        subareaOptions.keySet()
    )) {
      return addChangeModelAndView(
          correction,
          form,
          subareaOptions,
          LicencePositionCorrectionRouteUtil.getPositionPageUrl(correctionPosition)
      );
    }

    var changeType = AddPositionChangeType.valueOf(form.getChangeType());

    return switch (changeType) {
      case ADMINISTRATOR, LICENSEE, STORAGE_OPERATOR, EXPLORATION_OPERATOR ->
          ReverseRouter.redirect(on(ChangeFormController.class)
              .addToPosition(correctionPosition, changeType, null, null, null));
      case SET_EQUITY -> ReverseRouter.redirect(on(LicencePositionSetEquityController.class)
          .renderForPosition(correctionPosition));
      case TRANSFER_EQUITY -> ReverseRouter.redirect(on(LicencePositionTransferEquityController.class)
          .renderForPosition(correctionPosition));
      case PARTIAL_SURRENDER -> partialSurrenderCorrectionService
          .stageSingleBlockSurrender(correctionPosition)
          .map(singleBlockSurrender -> redirectToDefineArea(correction, singleBlockSurrender))
          .orElseGet(() -> ReverseRouter.redirect(on(LicencePositionPartialSurrenderController.class)
              .renderForPosition(correctionPosition)));
      case SUBAREA -> ReverseRouter.redirect(on(LicencePositionSubareaChangeStartController.class)
          .renderForPosition(correctionPosition));
      case SUBAREA_SET_EQUITY -> ReverseRouter.redirect(on(LicencePositionSubareaSetEquityController.class)
          .renderForPosition(correctionPosition, form.getSubarea()));
    };
  }

  private ModelAndView addChangeModelAndView(
      LicenceCorrection correction,
      AddPositionChangeForm form,
      Map<String, String> subareaOptions,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/change/addChange")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("changeTypeOptions", availableChangeTypeOptions(correction))
        .addObject("subareaSetEquityChangeType", AddPositionChangeType.SUBAREA_SET_EQUITY.name())
        .addObject("subareaOptions", subareaOptions)
        .addObject("backLinkUrl", backLinkUrl);
  }

  private ModelAndView redirectToDefineArea(LicenceCorrection correction, SingleBlockSurrender singleBlockSurrender) {
    return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class).renderDefineArea(
        correction,
        singleBlockSurrender.licencePositionCorrection(),
        singleBlockSurrender.block().getId()
    ));
  }

  private Map<String, String> availableChangeTypeOptions(LicenceCorrection correction) {
    var availableChangeTypes = Arrays.stream(AddPositionChangeType.values())
        .filter(changeType -> changeType.isAvailableFor(correction.getLicence().getType()))
        .toList();

    return DisplayableEnumOptionUtil.getDisplayableOptions(availableChangeTypes);
  }
}