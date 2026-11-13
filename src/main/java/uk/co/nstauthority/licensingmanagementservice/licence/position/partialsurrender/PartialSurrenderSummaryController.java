package uk.co.nstauthority.licensingmanagementservice.licence.position.partialsurrender;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToLicence;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummaryContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/{licenceId}/timeline/{licencePositionId}/change/{changeId}/partial-surrender")
public class PartialSurrenderSummaryController {

  static final String PAGE_TITLE = "Partial surrender";

  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionSpatialService licencePositionSpatialService;
  private final PartialSurrenderSummarySectionService partialSurrenderSummarySectionService;

  PartialSurrenderSummaryController(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionSpatialService licencePositionSpatialService,
      PartialSurrenderSummarySectionService partialSurrenderSummarySectionService
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.partialSurrenderSummarySectionService = partialSurrenderSummarySectionService;
  }

  @GetMapping
  @LicencePositionBelongsToLicence
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  public ModelAndView renderPartialSurrenderSummary(
      Licence licence,
      LicencePosition licencePosition,
      LicencePositionChange change,
      ServiceUserDetail user
  ) {
    if (licencePosition.getStatus() != LicencePositionStatus.EXECUTED) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Licence position %s is not executed"
          .formatted(licencePosition.getId()));
    }

    var changeId = change.getId().toString();
    var surrender = partialSurrenderCorrectionService.getLiveSurrenderOrThrow(changeId);
    var stateBefore = licencePositionSpatialService.getExecutedStateGoingIntoChange(licencePosition, changeId);
    var sections = partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.LiveChange(null, licencePosition, changeId, surrender, stateBefore),
        user
    );

    return new ModelAndView("lms/licence/correction/change/partialSurrender/partialSurrenderReviewAndSubmit")
        .addObject("readOnly", true)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licence.getLicenceReference())
        .addObject("accordionId", changeId)
        .addObject("summarySections", sections)
        .addObject("backLinkUrl", ReverseRouter.route(on(LicencePositionController.class)
            .renderLicencePosition(licence, licencePosition, null, null)));
  }
}
