package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionStagedPayloadBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class PartialSurrenderReviewAndSubmitController {

  public static final String REVIEW_AND_SUBMIT_PAGE_TITLE = "Review and submit";

  private static final String BACK_LINK_TEXT = "Back";
  private static final String BACK_TO_TASK_LIST_LINK_TEXT = "Back to task list";
  static final String SUBMITTED_BANNER = "Partial surrender saved";

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final PartialSurrenderSummarySectionService partialSurrenderSummarySectionService;
  private final PartialSurrenderValidator partialSurrenderValidator;
  private final LicencePositionSpatialService licencePositionSpatialService;

  public PartialSurrenderReviewAndSubmitController(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderSummarySectionService partialSurrenderSummarySectionService,
      PartialSurrenderValidator partialSurrenderValidator,
      LicencePositionSpatialService licencePositionSpatialService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderSummarySectionService = partialSurrenderSummarySectionService;
    this.partialSurrenderValidator = partialSurrenderValidator;
    this.licencePositionSpatialService = licencePositionSpatialService;
  }

  @GetMapping("/position-staged-payload/{licencePositionStagedPayloadId}/partial-surrender/review-and-submit")
  @LicencePositionStagedPayloadBelongsToCorrection
  public ModelAndView renderReviewAndSubmit(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      ServiceUserDetail user
  ) {
    var stagedChangeId = partialSurrenderCorrectionService
        .getCommittedPartialSurrenderChangeId(licencePositionStagedPayload)
        .orElse(null);
    var surrender = partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(licencePositionStagedPayload);
    var stateGoingIntoChange =
        licencePositionSpatialService.getStateGoingIntoChange(licencePositionStagedPayload, stagedChangeId);
    var errorMessages = validate(
        surrender,
        stateGoingIntoChange,
        licencePositionStagedPayloadService.resolveEffectiveDate(licencePositionStagedPayload),
        stagedChangeId
    );

    return getReviewAndSubmitModelAndView(
        correction,
        licencePositionStagedPayload,
        surrender,
        stateGoingIntoChange,
        errorMessages,
        user
    );
  }

  @PostMapping("/position-staged-payload/{licencePositionStagedPayloadId}/partial-surrender/review-and-submit")
  @LicencePositionStagedPayloadBelongsToCorrection
  public ModelAndView submitReviewAndSubmit(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    var stagedChangeId = partialSurrenderCorrectionService
        .getCommittedPartialSurrenderChangeId(licencePositionStagedPayload)
        .orElse(null);
    var surrender = partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(licencePositionStagedPayload);
    var stateGoingIntoChange =
        licencePositionSpatialService.getStateGoingIntoChange(licencePositionStagedPayload, stagedChangeId);
    var errorMessages = validate(
        surrender,
        stateGoingIntoChange,
        licencePositionStagedPayloadService.resolveEffectiveDate(licencePositionStagedPayload),
        stagedChangeId
    );

    if (!errorMessages.isEmpty()) {
      return getReviewAndSubmitModelAndView(
          correction,
          licencePositionStagedPayload,
          surrender,
          stateGoingIntoChange,
          errorMessages,
          user
      );
    }

    NotificationBanner.newSuccessBannerWithHeader(SUBMITTED_BANNER, redirectAttributes);
    return LicencePositionStagedPayloadRouteUtil.redirectToPositionPage(licencePositionStagedPayload);
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/review-and-submit")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderReviewAndSubmitForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      ServiceUserDetail user
  ) {
    var changeId = change.getId().toString();
    var surrender =
        partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId);
    var stateGoingIntoChange =
        licencePositionSpatialService.getStateGoingIntoChange(correction, licencePosition.getId(), changeId);
    var errorMessages = validate(
        surrender,
        stateGoingIntoChange,
        licencePositionStagedPayloadService.getEffectivePositionDate(correction, licencePosition),
        changeId
    );

    return getReviewAndSubmitForCorrectingChangeModelAndView(
        correction,
        licencePosition,
        change,
        surrender,
        stateGoingIntoChange,
        errorMessages,
        user
    );
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/review-and-submit")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitReviewAndSubmitForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    var changeId = change.getId().toString();
    var surrender =
        partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId);
    var stateGoingIntoChange =
        licencePositionSpatialService.getStateGoingIntoChange(correction, licencePosition.getId(), changeId);
    var errorMessages = validate(
        surrender,
        stateGoingIntoChange,
        licencePositionStagedPayloadService.getEffectivePositionDate(correction, licencePosition),
        changeId
    );

    if (!errorMessages.isEmpty()) {
      return getReviewAndSubmitForCorrectingChangeModelAndView(
          correction,
          licencePosition,
          change,
          surrender,
          stateGoingIntoChange,
          errorMessages,
          user
      );
    }

    NotificationBanner.newSuccessBannerWithHeader(SUBMITTED_BANNER, redirectAttributes);
    return LicencePositionStagedPayloadRouteUtil.redirectToPositionPage(correction, licencePosition.getId());
  }

  private List<String> validate(
      PartialSurrenderOperation surrender,
      LicencePositionState stateGoingIntoChange,
      LocalDate positionDate,
      @Nullable String changeId
  ) {
    var formErrors = partialSurrenderValidator.validateForms(surrender, stateGoingIntoChange);
    return formErrors.isEmpty()
        ? validateSpatially(surrender, stateGoingIntoChange, positionDate, changeId)
        : formErrors;
  }

  private List<String> validateSpatially(
      PartialSurrenderOperation surrender,
      LicencePositionState stateGoingIntoChange,
      LocalDate positionDate,
      @Nullable String changeId
  ) {
    return partialSurrenderValidator.validateSpatially(
        surrender,
        stateGoingIntoChange,
        positionDate,
        changeId,
        partialSurrenderValidator.getBlockLineBoundingBoxes(Set.of(positionDate))
    );
  }

  private ModelAndView getReviewAndSubmitModelAndView(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      PartialSurrenderOperation surrender,
      LicencePositionState stateGoingIntoChange,
      List<String> errorMessages,
      ServiceUserDetail user
  ) {
    var taskListUrl = ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correction, licencePositionStagedPayload, null));
    var singleBlockSelectAreasUrl = singleBlockSelectAreasUrl(licencePositionStagedPayload);
    var sections = partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.Staged(licencePositionStagedPayload, surrender, stateGoingIntoChange),
        user
    );

    return reviewAndSubmitModelAndView(
        correction,
        licencePositionStagedPayload.getId(),
        sections,
        errorMessages,
        singleBlockSelectAreasUrl.orElse(taskListUrl),
        singleBlockSelectAreasUrl.isPresent() ? BACK_LINK_TEXT : BACK_TO_TASK_LIST_LINK_TEXT);
  }

  private ModelAndView getReviewAndSubmitForCorrectingChangeModelAndView(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      PartialSurrenderOperation surrender,
      LicencePositionState stateGoingIntoChange,
      List<String> errorMessages,
      ServiceUserDetail user
  ) {
    var changeId = change.getId().toString();
    var taskListUrl = ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correction, licencePosition, change, null));
    var singleBlockSelectAreasUrl = licencePositionStagedPayloadService
        .findUpdatePositionStagedPayload(correction, licencePosition)
        .filter(positionStagedPayload -> partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionStagedPayload)
            .filter(changeId::equals)
            .isPresent())
        .flatMap(this::singleBlockSelectAreasUrl);
    var sections = partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.LiveChange(
            correction,
            licencePosition,
            changeId,
            surrender,
            stateGoingIntoChange
        ),
        user
    );

    return reviewAndSubmitModelAndView(
        correction,
        changeId,
        sections,
        errorMessages,
        singleBlockSelectAreasUrl.orElse(taskListUrl),
        singleBlockSelectAreasUrl.isPresent() ? BACK_LINK_TEXT : BACK_TO_TASK_LIST_LINK_TEXT
    );
  }

  private Optional<String> singleBlockSelectAreasUrl(
      LicencePositionStagedPayload positionStagedPayload
  ) {
    return partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload)
        .map(block -> ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
            .renderSelectAreas(positionStagedPayload.getLicenceCorrection(), positionStagedPayload, block.getId())));
  }

  private ModelAndView reviewAndSubmitModelAndView(
      LicenceCorrection correction,
      Object accordionId,
      List<SummarySection> summarySections,
      List<String> errorMessages,
      String backLinkUrl,
      String backLinkText
  ) {
    var errorSummaryItems = IntStream.range(0, errorMessages.size())
        .mapToObj(index -> new ErrorSummaryItem(index, "summaryaccordion-" + accordionId, errorMessages.get(index)))
        .toList();

    return new ModelAndView("lms/licence/correction/change/partialSurrender/partialSurrenderReviewAndSubmit")
        .addObject("pageTitle", REVIEW_AND_SUBMIT_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("accordionId", accordionId)
        .addObject("summarySections", summarySections)
        .addObject("errorSummaryItems", errorSummaryItems)
        .addObject("backLinkUrl", backLinkUrl)
        .addObject("backLinkText", backLinkText);
  }
}
