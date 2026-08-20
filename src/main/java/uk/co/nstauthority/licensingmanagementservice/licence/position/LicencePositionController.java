package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/{licenceId}/timeline")
@Profile("enable-lms2")
public class LicencePositionController {

  private final LicenceTimelinePositionTab licenceTimelinePositionTab;
  private final TabbedLicencePageService tabbedLicencePageService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionViewService licencePositionViewService;

  LicencePositionController(
      LicenceTimelinePositionTab licenceTimelinePositionTab,
      TabbedLicencePageService tabbedLicencePageService,
      LicencePositionService licencePositionService,
      LicencePositionViewService licencePositionViewService
  ) {
    this.licenceTimelinePositionTab = licenceTimelinePositionTab;
    this.tabbedLicencePageService = tabbedLicencePageService;
    this.licencePositionService = licencePositionService;
    this.licencePositionViewService = licencePositionViewService;
  }

  @GetMapping
  public ModelAndView renderLicencePositionTimeline(
      Licence licence,
      ServiceUserDetail user
  ) {
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    if (executedChronologicalLicencePositions.isEmpty()) {
      return licencePositionsModelAndView(licence, LicencePositionPageView.empty(), user);
    }

    return ReverseRouter.redirect(on(this.getClass()).renderLicencePosition(
        licence, executedChronologicalLicencePositions.getLast().getId(), null)
    );
  }

  @GetMapping("/{licencePositionId}")
  public ModelAndView renderLicencePosition(
      Licence licence,
      @PathVariable UUID licencePositionId,
      ServiceUserDetail user
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(licence, licencePositionId);
    var licencePositionPageView = licencePositionViewService.getPositionPageView(licencePosition);

    return licencePositionsModelAndView(licence, licencePositionPageView, user);
  }

  private ModelAndView licencePositionsModelAndView(
      Licence licence,
      LicencePositionPageView licencePositionPageView,
      ServiceUserDetail user
  ) {
    var licencePositionsModelAndView = new ModelAndView("lms/licence/position/licencePositions")
        .addObject("licencePositionPageView", licencePositionPageView);

    tabbedLicencePageService.hydrateModel(licencePositionsModelAndView, licence, licenceTimelinePositionTab, user);

    return licencePositionsModelAndView;
  }
}