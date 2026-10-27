package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

// The decision executor's direction letter, plus the authorised signatory's sign step
@Controller
@RequestMapping("/mockups/upload-dsp/direction-letter")
@Profile("mockups")
public class DirectionLetterMockupController {

  private final UploadDspMockupJourneyStore journeyStore;

  DirectionLetterMockupController(UploadDspMockupJourneyStore journeyStore) {
    this.journeyStore = journeyStore;
  }

  @GetMapping
  ModelAndView renderDirectionLetter() {
    return getDirectionLetterModelAndView(new DirectionLetterForm());
  }

  @PostMapping
  ModelAndView sendForSignature(
      @ModelAttribute("form") DirectionLetterForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (!DirectionLetterMockupData.SIGNATORY_OPTIONS.containsKey(form.getSignatory())) {
      bindingResult.rejectValue("signatory", "signatory.required", "Select an authorised signatory");
      return getDirectionLetterModelAndView(form);
    }
    // The option text carries the role for the select, but messages and events only need the name
    var signatoryName = StringUtils.substringBefore(
        DirectionLetterMockupData.SIGNATORY_OPTIONS.get(form.getSignatory()), " (");
    journeyStore.update(journey -> journey.sendForSignature(signatoryName));
    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Direction letter sent to " + signatoryName + " for signature")
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewSignatory(null));
  }

  @GetMapping("/sign")
  ModelAndView renderSign() {
    var journey = journeyStore.journey();
    var signatoryViewUrl = ReverseRouter.route(on(UploadDspMockupController.class).renderOverviewSignatory(null));
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/directionLetterSign")
        .addObject("pageTitle", "Sign direction letter")
        .addObject("caption", UploadDspMockupController.CAPTION)
        .addObject("signatoryName", journey.signatoryName())
        .addObject("awaitingSignature",
            journey.signatureStatus() == UploadDspMockupJourney.SignatureStatus.AWAITING)
        .addObject("backUrl", signatoryViewUrl);

    var breadcrumbs = Breadcrumbs.builder("Sign direction letter")
        .addWorkAreaBreadcrumb()
        .addBreadcrumb(UploadDspMockupController.APPLICATION_REFERENCE, signatoryViewUrl)
        .build();
    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  @PostMapping("/sign")
  ModelAndView sign(RedirectAttributes redirectAttributes) {
    journeyStore.update(UploadDspMockupJourney::signLetter);
    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Direction letter signed")
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
  }

  // Signing errors or tardy responses: nudges the signatory, who is still waiting on the same request
  @GetMapping("/reminder")
  ModelAndView sendReminder(RedirectAttributes redirectAttributes) {
    var journey = journeyStore.journey();
    if (journey.signatureStatus() == UploadDspMockupJourney.SignatureStatus.AWAITING) {
      journeyStore.update(UploadDspMockupJourney::sendSignatureReminder);
      NotificationBanner.newSuccessBanner()
          .withHeadingContent("Reminder sent to " + journey.signatoryName())
          .applyTo(redirectAttributes);
    }
    return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
  }

  // Issues straight away: the letter is emailed from LMS to the licensees
  @GetMapping("/issue")
  ModelAndView issueLetter(RedirectAttributes redirectAttributes) {
    if (journeyStore.journey().signatureStatus() == UploadDspMockupJourney.SignatureStatus.SIGNED) {
      journeyStore.update(UploadDspMockupJourney::issueLetter);
      NotificationBanner.newSuccessBanner()
          .withHeadingContent("Direction letter issued for " + UploadDspMockupController.APPLICATION_REFERENCE)
          .applyTo(redirectAttributes);
    }
    return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
  }

  private ModelAndView getDirectionLetterModelAndView(DirectionLetterForm form) {
    var journey = journeyStore.journey();
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/directionLetter")
        .addObject("pageTitle", "Direction letter for Shell UK limited")
        .addObject("form", form)
        .addObject("signatoryOptions", DirectionLetterMockupData.SIGNATORY_OPTIONS)
        .addObject("deedRecorded", "DOV".equals(journey.recordedDocumentType()))
        .addObject("awaitingSignatory",
            journey.signatureStatus() == UploadDspMockupJourney.SignatureStatus.AWAITING
                ? journey.signatoryName()
                : "")
        .addObject("cancelUrl", UploadDspMockupController.executionUrl())
        .addObject("documentInstanceSectionsSummaryView", MockLetter.directionLetter())
        .addObject("accordionId", UUID.randomUUID().toString())
        .addObject("hasMoreThanOneSection", true)
        .addObject("userHasValidPermission", true)
        .addObject("errorList", List.of());

    var breadcrumbs = Breadcrumbs.builder("Direction letter")
        .addWorkAreaBreadcrumb()
        .addBreadcrumb(UploadDspMockupController.APPLICATION_REFERENCE, UploadDspMockupController.executionUrl())
        .build();
    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}
