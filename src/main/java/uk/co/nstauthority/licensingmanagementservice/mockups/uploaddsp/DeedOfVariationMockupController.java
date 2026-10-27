package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadComponentAttributes;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

// The deed is drafted offline, so LMS starts when the decision executor uploads the finished deed
@Controller
@RequestMapping("/mockups/upload-dsp/deed-of-variation")
@Profile("mockups")
public class DeedOfVariationMockupController {

  private static final String DEFAULT_SEND_BACK_REASON = "The signature page is missing";

  private final FileControllerHelperService fileControllerHelperService;
  private final UploadDspMockupJourneyStore journeyStore;

  DeedOfVariationMockupController(
      FileControllerHelperService fileControllerHelperService,
      UploadDspMockupJourneyStore journeyStore
  ) {
    this.fileControllerHelperService = fileControllerHelperService;
    this.journeyStore = journeyStore;
  }

  @GetMapping
  ModelAndView renderDeed() {
    if (journeyStore.journey().deedStatus() != UploadDspMockupJourney.DeedStatus.NOT_STARTED) {
      return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
    }
    return getDeedModelAndView(new DeedOfVariationForm());
  }

  // Not blocked on a missing licensee contact, so the mockup journey can continue
  @PostMapping
  ModelAndView sendToLicensees(
      @ModelAttribute("form") DeedOfVariationForm form,
      RedirectAttributes redirectAttributes
  ) {
    var signingDeadline = form.getSigningDeadline().getAsLocalDate().orElse(LocalDate.now().plusDays(28));
    journeyStore.update(journey -> journey.sendDeedToLicensees(signingDeadline));
    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Deed of variation sent to the licensees for signature")
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
  }

  // Each licensee prints, signs and uploads their own copy
  @GetMapping("/signed/{licensee}")
  ModelAndView renderSignedDeed(@PathVariable("licensee") int licensee) {
    if (!journeyStore.journey().canUploadSignedDeed(licensee)) {
      return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewIndustry(licensee, null));
    }
    return getSignedDeedModelAndView(new SignedDeedForm(), licensee);
  }

  @PostMapping("/signed/{licensee}")
  ModelAndView submitSignedDeed(@PathVariable("licensee") int licensee, RedirectAttributes redirectAttributes) {
    journeyStore.update(journey -> journey.receiveSignedDeed(licensee));
    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Signed deed of variation sent to the NSTA")
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewIndustry(licensee, null));
  }

  public record ScanRow(String licensee, String status, String actionText, String actionUrl) {
  }

  // The executor's soft copy check: each licensee's scan is checked on its own and the loop repeats until all pass
  @GetMapping("/signed-copies")
  ModelAndView renderSignedCopies() {
    var journey = journeyStore.journey();
    if (journey.deedStatus() != UploadDspMockupJourney.DeedStatus.LICENSEE_SIGNING) {
      return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
    }
    var rows = new ArrayList<ScanRow>();
    for (var i = 0; i < UploadDspMockupJourney.LICENSEES.size(); i++) {
      var name = UploadDspMockupJourney.LICENSEES.get(i).name();
      rows.add(switch (journey.scanStatus(i)) {
        case AWAITING_SCAN -> new ScanRow(name, "Awaiting the signed deed", "Upload a posted hardcopy",
            ReverseRouter.route(on(DeedOfVariationMockupController.class).renderUploadOnBehalf(i)));
        case SCAN_RECEIVED -> new ScanRow(name, "Signed deed received", "Check the signed deed",
            ReverseRouter.route(on(DeedOfVariationMockupController.class).renderCheck(i)));
        case SENT_BACK -> new ScanRow(name, "Sent back: " + journey.sendBackReason(i), "Upload a posted hardcopy",
            ReverseRouter.route(on(DeedOfVariationMockupController.class).renderUploadOnBehalf(i)));
        case ACCEPTED -> journey.isAwaitingHardcopy(i)
            ? new ScanRow(name, "Accepted, awaiting the hardcopies", "Record hardcopies received",
            ReverseRouter.route(on(DeedOfVariationMockupController.class).renderHardcopy(i)))
            : new ScanRow(name, journey.isHardcopyNotRequired(i)
            ? "Accepted, hardcopies not required"
            : "Accepted, hardcopies received", null, null);
      });
    }
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/signedCopies")
        .addObject("pageTitle", "Signed deeds and hardcopies")
        .addObject("caption", UploadDspMockupController.CAPTION)
        .addObject("rows", rows)
        .addObject("backUrl", UploadDspMockupController.executionUrl());
    return withBreadcrumbs(modelAndView, "Signed deeds and hardcopies", UploadDspMockupController.executionUrl());
  }

  @GetMapping("/signed-copies/{licensee}")
  ModelAndView renderCheck(@PathVariable("licensee") int licensee) {
    if (journeyStore.journey().scanStatus(licensee) != UploadDspMockupJourney.ScanStatus.SCAN_RECEIVED) {
      return ReverseRouter.redirect(on(this.getClass()).renderSignedCopies());
    }
    return getCheckModelAndView(new DeedCheckForm(), licensee, false);
  }

  @PostMapping("/signed-copies/{licensee}")
  ModelAndView submitCheck(
      @PathVariable("licensee") int licensee,
      @ModelAttribute("form") DeedCheckForm form,
      RedirectAttributes redirectAttributes
  ) {
    journeyStore.update(journey -> applyOutcome(journey, licensee, form, false));
    return redirectAfterCheck(licensee, redirectAttributes);
  }

  // The licensee posted a hardcopy instead of a scan, so the executor scans it, uploads it and passes or fails it
  @GetMapping("/signed-copies/{licensee}/upload")
  ModelAndView renderUploadOnBehalf(@PathVariable("licensee") int licensee) {
    if (!journeyStore.journey().canUploadSignedDeed(licensee)) {
      return ReverseRouter.redirect(on(this.getClass()).renderSignedCopies());
    }
    return getCheckModelAndView(new DeedCheckForm(), licensee, true);
  }

  @PostMapping("/signed-copies/{licensee}/upload")
  ModelAndView submitUploadOnBehalf(
      @PathVariable("licensee") int licensee,
      @ModelAttribute("form") DeedCheckForm form,
      RedirectAttributes redirectAttributes
  ) {
    journeyStore.update(journey -> {
      journey.receivePostedHardcopy(licensee);
      applyOutcome(journey, licensee, form, true);
    });
    return redirectAfterCheck(licensee, redirectAttributes);
  }

  // Nothing is validated: an outcome other than send back is treated as a pass
  private void applyOutcome(UploadDspMockupJourney journey, int licensee, DeedCheckForm form, boolean postedHardcopy) {
    if ("SEND_BACK".equals(form.getOutcome())) {
      journey.sendBackSignedDeed(licensee, StringUtils.defaultIfBlank(form.getReason(), DEFAULT_SEND_BACK_REASON));
    } else if ("ACCEPT_WITHOUT_HARDCOPY".equals(form.getOutcome())) {
      journey.acceptWithoutHardcopy(licensee);
    } else {
      journey.acceptSignedDeed(licensee, postedHardcopy);
    }
  }

  private ModelAndView redirectAfterCheck(int licensee, RedirectAttributes redirectAttributes) {
    var journey = journeyStore.journey();
    var name = UploadDspMockupJourney.LICENSEES.get(licensee).name();
    var accepted = journey.scanStatus(licensee) == UploadDspMockupJourney.ScanStatus.ACCEPTED;
    NotificationBanner.newSuccessBanner()
        .withHeadingContent(accepted
            ? "Signed deed of variation accepted for " + name
            : "Signed deed of variation sent back to " + name)
        .applyTo(redirectAttributes);
    return redirectToSignedCopiesOrOverview();
  }

  // Stays on the hub while any licensee still has something outstanding
  private ModelAndView redirectToSignedCopiesOrOverview() {
    return journeyStore.journey().deedStatus() == UploadDspMockupJourney.DeedStatus.LICENSEE_SIGNING
        ? ReverseRouter.redirect(on(this.getClass()).renderSignedCopies())
        : ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
  }

  // A licensee posts their hardcopies as soon as their own signed deed is accepted, without waiting for the others
  @GetMapping("/signed-copies/{licensee}/hardcopy")
  ModelAndView renderHardcopy(@PathVariable("licensee") int licensee) {
    if (!journeyStore.journey().isAwaitingHardcopy(licensee)) {
      return ReverseRouter.redirect(on(this.getClass()).renderSignedCopies());
    }
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/hardcopies")
        .addObject("pageTitle", "Record hardcopies received")
        .addObject("caption", UploadDspMockupController.CAPTION)
        .addObject("licenseeName", UploadDspMockupJourney.LICENSEES.get(licensee).name())
        .addObject("backUrl", signedCopiesUrl());
    return withBreadcrumbs(modelAndView, "Record hardcopies received", signedCopiesUrl());
  }

  @PostMapping("/signed-copies/{licensee}/hardcopy")
  ModelAndView recordHardcopy(@PathVariable("licensee") int licensee, RedirectAttributes redirectAttributes) {
    journeyStore.update(journey -> journey.receiveHardcopy(licensee));
    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Hardcopies recorded as received from " + UploadDspMockupJourney.LICENSEES.get(licensee).name())
        .applyTo(redirectAttributes);
    return redirectToSignedCopiesOrOverview();
  }

  // The NSTA has signed the hardcopies, so the executor emails the scanned conformed copy to the licensees
  @GetMapping("/complete")
  ModelAndView renderComplete() {
    if (journeyStore.journey().deedStatus() != UploadDspMockupJourney.DeedStatus.NSTA_SIGNING) {
      return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
    }
    return getCompleteModelAndView(new ExecutedDeedForm());
  }

  @PostMapping("/complete")
  ModelAndView complete(RedirectAttributes redirectAttributes) {
    journeyStore.update(UploadDspMockupJourney::sendConformedDeed);
    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Conformed deed of variation sent to the licensees")
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
  }

  @GetMapping("/posted")
  ModelAndView renderPosted() {
    if (journeyStore.journey().deedStatus() != UploadDspMockupJourney.DeedStatus.POSTING_BACK) {
      return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
    }
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/posted")
        .addObject("pageTitle", "Record hardcopies posted")
        .addObject("caption", UploadDspMockupController.CAPTION)
        .addObject("licensees", journeyStore.journey().licenseesWithHardcopies())
        .addObject("backUrl", UploadDspMockupController.executionUrl());
    return withBreadcrumbs(modelAndView, "Record hardcopies posted", UploadDspMockupController.executionUrl());
  }

  @PostMapping("/posted")
  ModelAndView recordPosted(RedirectAttributes redirectAttributes) {
    journeyStore.update(UploadDspMockupJourney::completeDeed);
    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Deed of variation completed for " + UploadDspMockupController.APPLICATION_REFERENCE)
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(UploadDspMockupController.class).renderOverviewExecution(null));
  }

  private ModelAndView getCheckModelAndView(DeedCheckForm form, int licensee, boolean onBehalf) {
    var title = onBehalf ? "Upload a posted hardcopy" : "Check the signed deed";
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/checkDeed")
        .addObject("pageTitle", title)
        .addObject("caption", UploadDspMockupController.CAPTION)
        .addObject("form", form)
        .addObject("onBehalf", onBehalf)
        .addObject("licenseeName", UploadDspMockupJourney.LICENSEES.get(licensee).name())
        .addObject("hardcopyUpload", fileUploadAttributes(form.getPostedHardcopy(), "form.postedHardcopy"))
        .addObject("backUrl", signedCopiesUrl());
    return withBreadcrumbs(modelAndView, title, signedCopiesUrl());
  }

  private String signedCopiesUrl() {
    return ReverseRouter.route(on(this.getClass()).renderSignedCopies());
  }

  private ModelAndView getDeedModelAndView(DeedOfVariationForm form) {
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/deedOfVariation")
        .addObject("pageTitle", "Deed of variation")
        .addObject("caption", UploadDspMockupController.CAPTION)
        .addObject("form", form)
        .addObject("deedUpload", fileUploadAttributes(form.getDeedOfVariation(), "form.deedOfVariation"))
        .addObject("licensees", UploadDspMockupJourney.LICENSEES)
        .addObject("hasLicenseeWithoutContact",
            UploadDspMockupJourney.LICENSEES.stream().anyMatch(licensee -> licensee.contact() == null))
        .addObject("backUrl", UploadDspMockupController.executionUrl());
    return withBreadcrumbs(modelAndView, "Deed of variation", UploadDspMockupController.executionUrl());
  }

  private ModelAndView getSignedDeedModelAndView(SignedDeedForm form, int licensee) {
    var signingDeadline = journeyStore.journey().signingDeadline();
    var industryUrl = UploadDspMockupController.industryUrl(licensee);
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/signedDeed")
        .addObject("pageTitle", "Upload the signed deed of variation")
        .addObject("caption", UploadDspMockupController.CAPTION)
        .addObject("form", form)
        .addObject("licenseeName", UploadDspMockupJourney.LICENSEES.get(licensee).name())
        .addObject("sendBackReason", journeyStore.journey().sendBackReason(licensee))
        .addObject("signingDeadline", DateFormatUtil.convertToDisplayText(signingDeadline))
        .addObject("signedUpload", fileUploadAttributes(form.getSignedCounterparts(), "form.signedCounterparts"))
        .addObject("backUrl", industryUrl);
    return withBreadcrumbs(modelAndView, "Upload the signed deed of variation", industryUrl);
  }

  private ModelAndView getCompleteModelAndView(ExecutedDeedForm form) {
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/completeDeed")
        .addObject("pageTitle", "Send the conformed deed")
        .addObject("caption", UploadDspMockupController.CAPTION)
        .addObject("form", form)
        .addObject("licensees", UploadDspMockupJourney.LICENSEES)
        .addObject("executedUpload", fileUploadAttributes(form.getExecutedDeed(), "form.executedDeed"))
        .addObject("backUrl", UploadDspMockupController.executionUrl());
    return withBreadcrumbs(modelAndView, "Send the conformed deed", UploadDspMockupController.executionUrl());
  }

  private ModelAndView withBreadcrumbs(ModelAndView modelAndView, String pageTitle, String applicationUrl) {
    var breadcrumbs = Breadcrumbs.builder(pageTitle)
        .addWorkAreaBreadcrumb()
        .addBreadcrumb(UploadDspMockupController.APPLICATION_REFERENCE, applicationUrl)
        .build();
    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);
    return modelAndView;
  }

  private FileUploadComponentAttributes fileUploadAttributes(List<UploadedFileForm> files, String path) {
    return fileControllerHelperService.fileUploadComponentAttributes(
        files,
        UploadDspMockupController.class,
        controller -> controller.downloadFile(null),
        controller -> controller.deleteFile(null),
        path
    );
  }
}
