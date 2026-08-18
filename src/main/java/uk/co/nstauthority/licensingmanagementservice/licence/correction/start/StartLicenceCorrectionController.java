package uk.co.nstauthority.licensingmanagementservice.licence.correction.start;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LicenceActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/{licenceId}/correction/start")
@LicenceActionEndPointInterceptorRule.ActionEndPoint(LicenceActionItem.START_CORRECTION)
@Profile("enable-lms2")
public class StartLicenceCorrectionController {

  private static final String PAGE_TITLE = "Start a licence correction";

  private final LicenceService licenceService;
  private final StartLicenceCorrectionFormValidator startLicenceCorrectionFormValidator;
  private final LicenceCorrectionService licenceCorrectionService;
  private final TabbedLicencePageService tabbedLicencePageService;

  public StartLicenceCorrectionController(
      LicenceService licenceService,
      StartLicenceCorrectionFormValidator startLicenceCorrectionFormValidator,
      LicenceCorrectionService licenceCorrectionService,
      TabbedLicencePageService tabbedLicencePageService
  ) {
    this.licenceService = licenceService;
    this.startLicenceCorrectionFormValidator = startLicenceCorrectionFormValidator;
    this.licenceCorrectionService = licenceCorrectionService;
    this.tabbedLicencePageService = tabbedLicencePageService;
  }

  @GetMapping
  public ModelAndView renderStartLicenceCorrection(
      Licence licence
  ) {
    return startLicenceCorrectionModelAndView(licence, new StartLicenceCorrectionForm());
  }

  @PostMapping
  ModelAndView startLicenceCorrection(
      Licence licence,
      @ModelAttribute("form") StartLicenceCorrectionForm form,
      BindingResult bindingResult,
      ServiceUserDetail serviceUserDetail,
      RedirectAttributes redirectAttributes
  ) {
    if (startLicenceCorrectionFormValidator.hasErrors(form, bindingResult)) {
      return startLicenceCorrectionModelAndView(licence, form);
    }

    var correction = licenceCorrectionService.startCorrection(
        licence,
        form.getCorrectionReference().getInputValue(),
        form.getReason().getInputValue(),
        serviceUserDetail
    );

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence correction started")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderCorrection(correction.getId(), null));
  }

  private ModelAndView startLicenceCorrectionModelAndView(Licence licence, StartLicenceCorrectionForm form) {
    return new ModelAndView("lms/licence/correction/startCorrection")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("form", form)
        .addObject("backLinkUrl", tabbedLicencePageService.getDefaultTabUrl(licence));
  }
}
