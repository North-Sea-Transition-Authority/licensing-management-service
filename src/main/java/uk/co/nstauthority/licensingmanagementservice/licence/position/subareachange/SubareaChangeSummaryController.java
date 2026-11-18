package uk.co.nstauthority.licensingmanagementservice.licence.position.subareachange;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.UnaryOperator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToLicence;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review.SubareaChangeSummaryContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review.SubareaChangeSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/{licenceId}/timeline/{licencePositionId}/change/{changeId}/subarea-change")
public class SubareaChangeSummaryController {

  static final UnaryOperator<String> PAGE_TITLE = "Subarea change for Block %s"::formatted;

  private final SubareaChangeService subareaChangeService;
  private final FeatureService featureService;
  private final LicencePositionSpatialService licencePositionSpatialService;
  private final SubareaChangeSummarySectionService subareaChangeSummarySectionService;

  public SubareaChangeSummaryController(
      SubareaChangeService subareaChangeService,
      FeatureService featureService,
      LicencePositionSpatialService licencePositionSpatialService,
      SubareaChangeSummarySectionService subareaChangeSummarySectionService
  ) {
    this.subareaChangeService = subareaChangeService;
    this.featureService = featureService;
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.subareaChangeSummarySectionService = subareaChangeSummarySectionService;
  }

  @GetMapping
  @LicencePositionBelongsToLicence
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(SubareaOperation.class)
  public ModelAndView renderSubareaChangeSummary(
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
    var operation = subareaChangeService.getLiveSubareaOperationOrThrow(change);
    var blockFeatureId = operation.blockFeatureId();
    var blockName = featureService.getFeatureOrThrow(blockFeatureId).getFeatureName();
    var stateBefore = licencePositionSpatialService.getExecutedStateGoingIntoChange(licencePosition, changeId);
    var sections = subareaChangeSummarySectionService.getSummarySections(
        new SubareaChangeSummaryContext.LiveChange(
            blockFeatureId,
            stateBefore.subareasOf(blockFeatureId),
            operation.transfers(),
            stateBefore,
            licencePositionSpatialService.getExecutedStateGoingIntoChange(licencePosition, null),
            null
        ),
        user
    );

    return new ModelAndView("lms/licence/correction/change/subarea/subareaChangeReview")
        .addObject("readOnly", true)
        .addObject("pageTitle", PAGE_TITLE.apply(blockName))
        .addObject("pageCaption", licence.getLicenceReference())
        .addObject("accordionId", changeId)
        .addObject("summarySections", sections)
        .addObject("backLinkUrl", ReverseRouter.route(on(LicencePositionController.class)
            .renderLicencePosition(licence, licencePosition, null, null)));
  }
}
