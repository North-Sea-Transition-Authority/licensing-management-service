package uk.co.nstauthority.licensingmanagementservice.licence.pearsresync;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LicenceActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.LicenceWritebackService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

/**
 * Rebuilds a production licence out of its PEARS history from the licence page, as the
 * {@code pears-licence-writeback} actuator endpoint does. Only registered where the PEARS datasource is configured.
 */
@Controller
@RequestMapping("licences/{licenceId}/pears-resync")
@LicenceActionEndPointInterceptorRule.ActionEndPoint(LicenceActionItem.RESYNC_FROM_PEARS)
@ConditionalOnPearsDataSource
public class PearsLicenceResyncController {

  private final LicenceService licenceService;
  private final LicenceWritebackService licenceWritebackService;
  private final TabbedLicencePageService tabbedLicencePageService;

  PearsLicenceResyncController(
      LicenceService licenceService,
      LicenceWritebackService licenceWritebackService,
      TabbedLicencePageService tabbedLicencePageService
  ) {
    this.licenceService = licenceService;
    this.licenceWritebackService = licenceWritebackService;
    this.tabbedLicencePageService = tabbedLicencePageService;
  }

  @GetMapping
  public ModelAndView renderResyncConfirmation(Licence licence) {
    return new ModelAndView("lms/licence/pearsresync/resyncLicenceFromPears")
        .addObject("pageTitle", "Are you sure you want to resync %s from PEARS?".formatted(licence.getLicenceReference()))
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("cancelUrl", tabbedLicencePageService.getDefaultTabUrl(licence));
  }

  @PostMapping
  ModelAndView resync(Licence licence, RedirectAttributes redirectAttributes) {
    var result = licenceWritebackService.resync(licence);

    // A writeback that builds anything builds at least one position, so none means PEARS held nothing to build from
    // and the licence was left as it was.
    var banner = result.positions() == 0
        ? NotificationBanner.newInfoBanner().withHeadingContent("Licence not resynced from PEARS")
        : NotificationBanner.newSuccessBanner().withHeadingContent("Licence resynced from PEARS");

    banner.withOtherContent(result.result())
        .applyTo(redirectAttributes);

    return ReverseRouter.redirectToUrl(tabbedLicencePageService.getDefaultTabUrl(licence));
  }
}
