package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummaryContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class PartialSurrenderTaskListController {

  public static final String TASK_LIST_PAGE_TITLE = "Partial surrender";
  public static final String REVIEW_AND_SUBMIT_PAGE_TITLE = "Review and submit";

  private static final String BACK_LINK_TEXT = "Back";
  private static final String BACK_TO_TASK_LIST_LINK_TEXT = "Back to task list";
  static final String SUBMITTED_BANNER = "Partial surrender saved";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final PartialSurrenderTaskListService partialSurrenderTaskListService;
  private final PartialSurrenderSummarySectionService partialSurrenderSummarySectionService;
  private final PartialSurrenderValidator partialSurrenderValidator;
  private final LicencePositionSpatialService licencePositionSpatialService;

  public PartialSurrenderTaskListController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderTaskListService partialSurrenderTaskListService,
      PartialSurrenderSummarySectionService partialSurrenderSummarySectionService,
      PartialSurrenderValidator partialSurrenderValidator,
      LicencePositionSpatialService licencePositionSpatialService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderTaskListService = partialSurrenderTaskListService;
    this.partialSurrenderSummarySectionService = partialSurrenderSummarySectionService;
    this.partialSurrenderValidator = partialSurrenderValidator;
    this.licencePositionSpatialService = licencePositionSpatialService;
  }

  @GetMapping("/position-correction/{licencePositionCorrectionId}/partial-surrender/task-list")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderTaskList(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      ServiceUserDetail user
  ) {
    partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(licencePositionCorrection);

    var correctedLiveChangeId = partialSurrenderCorrectionService.findCorrectedLiveChangeId(licencePositionCorrection);
    if (correctedLiveChangeId.isPresent()) {
      return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class).renderForCorrectingChange(
          correction,
          licencePositionCorrection.getTargetLicencePosition(),
          new LicencePositionChange(UUID.fromString(correctedLiveChangeId.get())),
          null));
    }

    var singleBlock = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(licencePositionCorrection);
    if (singleBlock.isPresent()) {
      return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
          .renderDefineArea(correction, licencePositionCorrection, singleBlock.get().getId()));
    }

    return taskListModelAndView(
        correction,
        partialSurrenderTaskListService.getTaskListSections(
            new PartialSurrenderTaskListContext.Staged(licencePositionCorrection), user),
        positionReference(licencePositionCorrection),
        DateUtil.formatLongDate(licencePositionCorrectionService.resolveEffectiveDate(licencePositionCorrection)),
        LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, licencePositionCorrection));
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/task-list")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      ServiceUserDetail user
  ) {
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);

    // If only a single block, go to the PartialSurrenderController to stage a single block correction
    if (partialSurrenderCorrectionService
        .findSingleBlockNotOperatedOn(correction, licencePosition, positionCorrection, change.getId().toString())
        .isPresent()
    ) {
      return ReverseRouter.redirect(on(LicencePositionPartialSurrenderController.class)
          .renderForCorrectingChange(correction, licencePosition, change));
    }

    return taskListModelAndView(
        correction,
        partialSurrenderTaskListService.getTaskListSections(
            new PartialSurrenderTaskListContext.LiveChange(correction, licencePosition, change), user),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        licencePosition.getFormattedPositionDate(),
        ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, licencePosition, null)));
  }

  @GetMapping("/position-correction/{licencePositionCorrectionId}/partial-surrender/review-and-submit")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderReviewAndSubmit(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      ServiceUserDetail user
  ) {
    var stagedChangeId = partialSurrenderCorrectionService
        .getCommittedPartialSurrenderChangeId(licencePositionCorrection)
        .orElse(null);
    var surrender = partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(licencePositionCorrection);
    var stateGoingIntoChange =
        licencePositionSpatialService.getStateGoingIntoChange(licencePositionCorrection, stagedChangeId);
    var errorMessages = validate(
        surrender,
        stateGoingIntoChange,
        licencePositionCorrectionService.resolveEffectiveDate(licencePositionCorrection),
        stagedChangeId
    );

    return getReviewAndSubmitModelAndView(
        correction,
        licencePositionCorrection,
        surrender,
        stateGoingIntoChange,
        errorMessages,
        user
    );
  }

  @PostMapping("/position-correction/{licencePositionCorrectionId}/partial-surrender/review-and-submit")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitReviewAndSubmit(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    var stagedChangeId = partialSurrenderCorrectionService
        .getCommittedPartialSurrenderChangeId(licencePositionCorrection)
        .orElse(null);
    var surrender = partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(licencePositionCorrection);
    var stateGoingIntoChange =
        licencePositionSpatialService.getStateGoingIntoChange(licencePositionCorrection, stagedChangeId);
    var errorMessages = validate(
        surrender,
        stateGoingIntoChange,
        licencePositionCorrectionService.resolveEffectiveDate(licencePositionCorrection),
        stagedChangeId
    );

    if (!errorMessages.isEmpty()) {
      return getReviewAndSubmitModelAndView(
          correction,
          licencePositionCorrection,
          surrender,
          stateGoingIntoChange,
          errorMessages,
          user
      );
    }

    NotificationBanner.newSuccessBannerWithHeader(SUBMITTED_BANNER, redirectAttributes);
    return ReverseRouter.redirectToUrl(
        LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, licencePositionCorrection));
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
        licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
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
        licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
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
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition, null));
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
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation surrender,
      LicencePositionState stateGoingIntoChange,
      List<String> errorMessages,
      ServiceUserDetail user
  ) {
    var taskListUrl = ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correction, licencePositionCorrection, null));
    var singleBlockSelectAreasUrl = singleBlockSelectAreasUrl(correction, licencePositionCorrection);
    var sections = partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.Staged(licencePositionCorrection, surrender, stateGoingIntoChange),
        user
    );

    return reviewAndSubmitModelAndView(
        correction,
        licencePositionCorrection.getId(),
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
    var singleBlockSelectAreasUrl = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .filter(positionCorrection -> partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection)
            .filter(changeId::equals)
            .isPresent())
        .flatMap(positionCorrection -> singleBlockSelectAreasUrl(correction, positionCorrection));
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
      LicenceCorrection correction,
      LicencePositionCorrection positionCorrection
  ) {
    return partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection)
        .map(block -> ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
            .renderSelectAreas(correction, positionCorrection, block.getId())));
  }

  private ModelAndView taskListModelAndView(
      LicenceCorrection correction,
      List<TaskListSection> sections,
      String positionReference,
      String positionDate,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/change/partialSurrender/partialSurrenderTaskList")
        .addObject("pageTitle", TASK_LIST_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("positionReference", positionReference)
        .addObject("positionDate", positionDate)
        .addObject("taskListSections", sections)
        .addObject("backLinkUrl", backLinkUrl);
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

  private String positionReference(LicencePositionCorrection positionCorrection) {
    return switch (positionCorrection.getPayload()) {
      case CreateLicencePositionPayload create -> create.correctionReference();
      case UpdateLicencePositionPayload ignored ->
          positionCorrection.getTargetLicencePosition().getLicenceTransaction().getRegulatorReference();
    };
  }
}
