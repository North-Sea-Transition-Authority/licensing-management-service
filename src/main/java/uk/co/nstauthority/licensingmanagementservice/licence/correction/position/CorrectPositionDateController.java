package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{licencePositionId}/correct-position-date")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@LicencePositionBelongsToCorrectionLicence
public class CorrectPositionDateController {

  private final CorrectPositionDateFormValidator correctPositionDateFormValidator;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  public CorrectPositionDateController(
          CorrectPositionDateFormValidator correctPositionDateFormValidator,
          LicencePositionStagedPayloadService licencePositionStagedPayloadService,
          PartialSurrenderCorrectionService partialSurrenderCorrectionService
  ) {
    this.correctPositionDateFormValidator = correctPositionDateFormValidator;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
  }

  @GetMapping
  public ModelAndView renderCorrectLicencePositionStagedPayloadDate(
          LicenceCorrection correction,
          LicencePosition licencePosition
  ) {
    return correctPositionStagedPayloadDateModelAndView(correction, licencePosition, new CorrectPositionDateForm());
  }

  @PostMapping
  ModelAndView correctLicencePositionStagedPayloadDate(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") CorrectPositionDateForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (correctPositionDateFormValidator.hasErrors(form, bindingResult)) {
      return correctPositionStagedPayloadDateModelAndView(correction, licencePosition, form);
    }

    var positionStagedPayload = licencePositionStagedPayloadService.correctPositionDate(
        correction,
        licencePosition,
        form.getCorrectPositionDate().getAsLocalDate().orElseThrow()
    );

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocks(positionStagedPayload);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence position correction date updated")
        .applyTo(redirectAttributes);

    return LicencePositionStagedPayloadRouteUtil.redirectToPositionPage(correction, licencePosition.getId());
  }

  private ModelAndView correctPositionStagedPayloadDateModelAndView(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      CorrectPositionDateForm form
  ) {
    var pageTitle = "Correct the date of a licence position";
    return new ModelAndView("lms/licence/correction/correctPositionStagedPayloadDate")
        .addObject("pageTitle", pageTitle)
        .addObject("regulatorReference", licencePosition.getLicenceTransaction().getRegulatorReference())
        .addObject("currentPositionDate", licencePosition.getFormattedPositionDate())
        .addObject("form", form)
        .addObject("backLinkUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class).renderCorrection(correction, null)));
  }
}
