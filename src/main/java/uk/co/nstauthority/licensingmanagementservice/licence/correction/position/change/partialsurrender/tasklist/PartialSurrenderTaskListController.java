package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummaryContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/position-correction/{licencePositionCorrectionId}/partial-surrender")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class PartialSurrenderTaskListController {

  public static final String TASK_LIST_PAGE_TITLE = "Partial surrender";
  public static final String REVIEW_AND_SUBMIT_PAGE_TITLE = "Review the partial surrender before submitting";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final PartialSurrenderTaskListService partialSurrenderTaskListService;
  private final PartialSurrenderSummarySectionService partialSurrenderSummarySectionService;

  public PartialSurrenderTaskListController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderTaskListService partialSurrenderTaskListService,
      PartialSurrenderSummarySectionService partialSurrenderSummarySectionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderTaskListService = partialSurrenderTaskListService;
    this.partialSurrenderSummarySectionService = partialSurrenderSummarySectionService;
  }

  @GetMapping("/task-list")
  public ModelAndView renderTaskList(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      ServiceUserDetail user
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);

    partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection);

    var sections = partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext(positionCorrection), user);

    return new ModelAndView("lms/licence/correction/change/partialSurrender/partialSurrenderTaskList")
        .addObject("pageTitle", TASK_LIST_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("positionReference", positionReference(positionCorrection))
        .addObject("positionDate", DateUtil.formatLongDate(
            licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)))
        .addObject("taskListSections", sections)
        .addObject("backLinkUrl", positionUrl(correctionId, positionCorrection));
  }

  @GetMapping("/review-and-submit")
  public ModelAndView renderReviewAndSubmit(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      ServiceUserDetail user
  ) {
    var licencePositionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var taskListUrl = ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correctionId, licencePositionCorrectionId, null, null));
    var sections = partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext(licencePositionCorrection),
        user
    );

    return new ModelAndView("lms/licence/correction/change/partialSurrenderReviewAndSubmit")
        .addObject("pageTitle", REVIEW_AND_SUBMIT_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("summarySections", sections)
        .addObject("accordionId", licencePositionCorrection.getId())
        .addObject("backLinkUrl", taskListUrl);
  }

  private String positionReference(LicencePositionCorrection positionCorrection) {
    return switch (positionCorrection.getPayload()) {
      case CreateLicencePositionPayload create -> create.correctionReference();
      case UpdateLicencePositionPayload ignored ->
          positionCorrection.getTargetLicencePosition().getLicenceTransaction().getRegulatorReference();
    };
  }

  private String positionUrl(UUID correctionId, LicencePositionCorrection positionCorrection) {
    return switch (positionCorrection.getChangeType()) {
      case ADD_POSITION -> ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderAddedPosition(correctionId, positionCorrection.getId(), null));
      case UPDATE_POSITION -> ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderLicencePosition(correctionId, positionCorrection.getTargetLicencePosition().getId(), null));
      case REMOVE_POSITION -> throw new IllegalStateException(
          "Licence position correction %s removes a position so cannot carry a partial surrender"
              .formatted(positionCorrection.getId()));
    };
  }
}
