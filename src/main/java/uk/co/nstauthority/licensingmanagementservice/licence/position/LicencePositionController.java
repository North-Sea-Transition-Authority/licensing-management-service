package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/{licenceId}/timeline")
@Profile("enable-lms2")
public class LicencePositionController {

  //TODO LMS2-52: Access to timeline, licence position and schedule information for a licence
  private final LicencePositionService licencePositionService;
  private final LicencePositionViewService licencePositionViewService;
  private final LicenceService licenceService;

  public LicencePositionController(
      LicencePositionService licencePositionService,
      LicencePositionViewService licencePositionViewService,
      LicenceService licenceService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionViewService = licencePositionViewService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderLicencePositionTimeline(
      Licence licence
  ) {
    var executedChronologicalLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    if (executedChronologicalLicencePositions.isEmpty()) {
      return licencePositionsModelAndView(licence, LicencePositionPageView.empty());
    }

    return ReverseRouter.redirect(on(this.getClass()).renderLicencePosition(
        licence, executedChronologicalLicencePositions.getLast().getId())
    );
  }

  @GetMapping("/{licencePositionId}")
  public ModelAndView renderLicencePosition(
      Licence licence,
      @PathVariable UUID licencePositionId
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(licence, licencePositionId);
    var licencePositionPageView = licencePositionViewService.getPositionPageView(licencePosition);

    return licencePositionsModelAndView(licence, licencePositionPageView);
  }

  private ModelAndView licencePositionsModelAndView(Licence licence, LicencePositionPageView licencePositionPageView) {
    return new ModelAndView("lms/licence/position/licencePositions")
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("licencePositionPageView", licencePositionPageView);
  }
}