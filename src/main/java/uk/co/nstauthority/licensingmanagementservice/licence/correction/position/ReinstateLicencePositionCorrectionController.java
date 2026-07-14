package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCanBeReinstantiated;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{licencePositionId}/reinstate")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
@LicencePositionCanBeReinstantiated
public class ReinstateLicencePositionCorrectionController {

  private static final String PAGE_TITLE = "Are you sure you want to reinstate this position?";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;

  public ReinstateLicencePositionCorrectionController(
      LicencePositionCorrectionService licencePositionCorrectionService, LicencePositionService licencePositionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
  }

  @GetMapping
  public ModelAndView renderReinstatePosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    return reinstatePositionModelAndView(correction, licencePosition);
  }

  @PostMapping
  ModelAndView reinstatePosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    licencePositionCorrectionService.reinstateDeletedPositionCorrection(correction, licencePosition);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence correction position reinstated")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderCorrection(correction.getId(), null));
  }

  private ModelAndView reinstatePositionModelAndView(LicenceCorrection correction, LicencePosition licencePosition) {
    return new ModelAndView("lms/licence/correction/reinstatePosition")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("positionDate", licencePosition.getFormattedPositionDate())
        .addObject("correctionReference", correction.getCorrectionReference())
        .addObject("cancelUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(correction.getId(), null)));
  }
}