package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionStagedPayloadBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class PartialSurrenderTaskListController {

  public static final String TASK_LIST_PAGE_TITLE = "Partial surrender";

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final PartialSurrenderTaskListService partialSurrenderTaskListService;

  public PartialSurrenderTaskListController(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderTaskListService partialSurrenderTaskListService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderTaskListService = partialSurrenderTaskListService;
  }

  @GetMapping("/position-staged-payload/{licencePositionStagedPayloadId}/partial-surrender/task-list")
  @LicencePositionStagedPayloadBelongsToCorrection
  public ModelAndView renderTaskList(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      ServiceUserDetail user
  ) {
    partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(licencePositionStagedPayload);

    var correctedLiveChangeId = partialSurrenderCorrectionService.findCorrectedLiveChangeId(licencePositionStagedPayload);
    if (correctedLiveChangeId.isPresent()) {
      return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class).renderForCorrectingChange(
          correction,
          licencePositionStagedPayload.getTargetLicencePosition(),
          new LicencePositionChange(UUID.fromString(correctedLiveChangeId.get())),
          null));
    }

    var singleBlock = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(licencePositionStagedPayload);
    if (singleBlock.isPresent()) {
      return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
          .renderDefineArea(correction, licencePositionStagedPayload, singleBlock.get().getId()));
    }

    return taskListModelAndView(
        correction,
        partialSurrenderTaskListService.getTaskListSections(
            new PartialSurrenderTaskListContext.Staged(licencePositionStagedPayload), user),
        positionReference(licencePositionStagedPayload),
        DateUtil.formatLongDate(licencePositionStagedPayloadService.resolveEffectiveDate(licencePositionStagedPayload)),
        LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(licencePositionStagedPayload));
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
    var positionStagedPayload = licencePositionStagedPayloadService
        .findUpdatePositionStagedPayload(correction, licencePosition)
        .orElse(null);

    // If only a single block, go to the PartialSurrenderController to stage a single block correction
    if (partialSurrenderCorrectionService
        .findSingleBlockNotOperatedOn(correction, licencePosition, positionStagedPayload, change.getId().toString())
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
        LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(correction, licencePosition.getId()));
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

  private String positionReference(LicencePositionStagedPayload positionStagedPayload) {
    return switch (positionStagedPayload.getPayload()) {
      case CreateLicencePositionPayload create -> create.correctionReference();
      case UpdateLicencePositionPayload ignored ->
          positionStagedPayload.getTargetLicencePosition().getLicenceTransaction().getRegulatorReference();
    };
  }
}
