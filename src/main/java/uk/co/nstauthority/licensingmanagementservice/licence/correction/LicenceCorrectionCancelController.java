package uk.co.nstauthority.licensingmanagementservice.licence.correction;

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
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/cancel")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
class LicenceCorrectionCancelController {

  private final LicenceCorrectionService licenceCorrectionService;

  LicenceCorrectionCancelController(LicenceCorrectionService licenceCorrectionService) {
    this.licenceCorrectionService = licenceCorrectionService;
  }

  @GetMapping
  ModelAndView renderCancelCorrection(@PathVariable UUID correctionId,
                                      @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection) {
    return new ModelAndView("lms/licence/correction/cancelCorrection")
        .addObject("correctionReference", licenceCorrection.getCorrectionReference())
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correctionId, null)));
  }

  @PostMapping
  ModelAndView processCancelCorrection(@PathVariable UUID correctionId,
                                       @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection,
                                       RedirectAttributes redirectAttributes) {
    licenceCorrectionService.cancelCorrection(licenceCorrection);
    NotificationBanner.newSuccessBannerWithHeader(
        "Correction %s cancelled".formatted(licenceCorrection.getCorrectionReference()),
        redirectAttributes
    );

    return ReverseRouter.redirect(on(LicenceOverviewController.class)
        .renderLicenceOverview(licenceCorrection.getLicence().getId(), null, null, null));
  }
}
