package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{licencePositionId}/correct-position-date")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
public class CorrectPositionDateController {

  private final CorrectPositionDateFormValidator correctPositionDateFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;

  public CorrectPositionDateController(
          CorrectPositionDateFormValidator correctPositionDateFormValidator,
          LicencePositionCorrectionService licencePositionCorrectionService,
          LicencePositionService licencePositionService
  ) {
    this.correctPositionDateFormValidator = correctPositionDateFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
  }

  @GetMapping
  public ModelAndView renderCorrectLicencePositionCorrectionDate(
          @PathVariable UUID correctionId,
          @PathVariable UUID licencePositionId,
          @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    return correctPositionCorrectionDateModelAndView(correction, licencePosition, new CorrectPositionDateForm());
  }

  @PostMapping
  ModelAndView correctLicencePositionCorrectionDate(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") CorrectPositionDateForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);

    if (correctPositionDateFormValidator.hasErrors(form, bindingResult)) {
      return correctPositionCorrectionDateModelAndView(correction, licencePosition, form);
    }

    licencePositionCorrectionService.correctPositionDate(
        correction,
        licencePosition,
        form.getCorrectPositionDate().getAsLocalDate().orElseThrow()
    );

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence position correction date updated")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderCorrection(correction.getId(), null));
  }

  private ModelAndView correctPositionCorrectionDateModelAndView(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      CorrectPositionDateForm form
  ) {
    var pageTitle = "Correct the date of a licence position";
    return new ModelAndView("lms/licence/correction/correctPositionCorrectionDate")
        .addObject("pageTitle", pageTitle)
        .addObject("regulatorReference", licencePosition.getLicenceTransaction().getRegulatorReference())
        .addObject("currentPositionDate", licencePosition.getFormattedPositionDate())
        .addObject("form", form)
        .addObject("backLinkUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(correction.getId(), null)));
  }
}
