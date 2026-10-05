package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummaryContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
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

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final PartialSurrenderTaskListService partialSurrenderTaskListService;
  private final PartialSurrenderSummarySectionService partialSurrenderSummarySectionService;
  private final LicencePositionService licencePositionService;

  public PartialSurrenderTaskListController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderTaskListService partialSurrenderTaskListService,
      PartialSurrenderSummarySectionService partialSurrenderSummarySectionService,
      LicencePositionService licencePositionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderTaskListService = partialSurrenderTaskListService;
    this.partialSurrenderSummarySectionService = partialSurrenderSummarySectionService;
    this.licencePositionService = licencePositionService;
  }

  @GetMapping("/position-correction/{licencePositionCorrectionId}/partial-surrender/task-list")
  public ModelAndView renderTaskList(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      ServiceUserDetail user
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);

    partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection);

    var correctedLiveChangeId = partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection);
    if (correctedLiveChangeId.isPresent()) {
      return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class).renderForCorrectingChange(
          correctionId,
          positionCorrection.getTargetLicencePosition().getId(),
          correctedLiveChangeId.get(),
          null,
          null));
    }

    var singleBlock = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection);
    if (singleBlock.isPresent()) {
      return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
          .renderDefineArea(correctionId, licencePositionCorrectionId, singleBlock.get().getId(), null));
    }

    return taskListModelAndView(
        correction,
        partialSurrenderTaskListService.getTaskListSections(
            new PartialSurrenderTaskListContext.Staged(positionCorrection), user),
        positionReference(positionCorrection),
        DateUtil.formatLongDate(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)),
        LicencePositionCorrectionRouteUtil.getPositionPageUrl(correctionId, positionCorrection));
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/task-list")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  public ModelAndView renderForCorrectingChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      ServiceUserDetail user
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);

    // If only a single block, go to the PartialSurrenderController to stage a single block correction
    if (partialSurrenderCorrectionService
        .findSingleBlockNotOperatedOn(correction, licencePosition, positionCorrection, changeId)
        .isPresent()
    ) {
      return ReverseRouter.redirect(on(LicencePositionPartialSurrenderController.class)
          .renderForCorrectingChange(correctionId, licencePositionId, changeId, null));
    }

    return taskListModelAndView(
        correction,
        partialSurrenderTaskListService.getTaskListSections(
            new PartialSurrenderTaskListContext.LiveChange(correction, licencePosition, changeId), user),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        licencePosition.getFormattedPositionDate(),
        ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correctionId, licencePosition.getId(), null)));
  }

  @GetMapping("/position-correction/{licencePositionCorrectionId}/partial-surrender/review-and-submit")
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
    var singleBlockSelectAreasUrl = singleBlockSelectAreasUrl(correctionId, licencePositionCorrection);
    var sections = partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.Staged(licencePositionCorrection),
        user
    );

    return reviewAndSubmitModelAndView(
        correction,
        licencePositionCorrectionId,
        sections,
        partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(licencePositionCorrection),
        singleBlockSelectAreasUrl.orElse(taskListUrl),
        singleBlockSelectAreasUrl.isPresent() ? BACK_LINK_TEXT : BACK_TO_TASK_LIST_LINK_TEXT);
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/review-and-submit")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  public ModelAndView renderReviewAndSubmitForCorrectingChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      ServiceUserDetail user
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var surrender = partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId);
    var taskListUrl = ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correctionId, licencePositionId, changeId, null, null));
    var singleBlockSelectAreasUrl = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .filter(positionCorrection -> partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection)
            .filter(changeId::equals)
            .isPresent())
        .flatMap(positionCorrection -> singleBlockSelectAreasUrl(correctionId, positionCorrection));

    var sections = partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.LiveChange(correction, licencePosition, changeId),
        user
    );

    return reviewAndSubmitModelAndView(
        correction,
        changeId,
        sections,
        partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(surrender),
        singleBlockSelectAreasUrl.orElse(taskListUrl),
        singleBlockSelectAreasUrl.isPresent() ? BACK_LINK_TEXT : BACK_TO_TASK_LIST_LINK_TEXT
    );
  }

  private Optional<String> singleBlockSelectAreasUrl(UUID correctionId, LicencePositionCorrection positionCorrection) {
    return partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection)
        .map(block -> ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
            .renderSelectAreas(correctionId, positionCorrection.getId(), block.getId(), null)));
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
      boolean allSurrenderedBlocksAreFull,
      String backLinkUrl,
      String backLinkText
  ) {
    return new ModelAndView("lms/licence/correction/change/partialSurrender/partialSurrenderReviewAndSubmit")
        .addObject("pageTitle", REVIEW_AND_SUBMIT_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("accordionId", accordionId)
        .addObject("summarySections", summarySections)
        .addObject("allSurrenderedBlocksAreFull", allSurrenderedBlocksAreFull)
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
