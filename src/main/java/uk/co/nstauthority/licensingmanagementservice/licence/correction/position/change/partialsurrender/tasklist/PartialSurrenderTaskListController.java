package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class PartialSurrenderTaskListController {

  public static final String TASK_LIST_PAGE_TITLE = "Partial surrender";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final PartialSurrenderTaskListService partialSurrenderTaskListService;

  public PartialSurrenderTaskListController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderTaskListService partialSurrenderTaskListService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderTaskListService = partialSurrenderTaskListService;
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

  private String positionReference(LicencePositionCorrection positionCorrection) {
    return switch (positionCorrection.getPayload()) {
      case CreateLicencePositionPayload create -> create.correctionReference();
      case UpdateLicencePositionPayload ignored ->
          positionCorrection.getTargetLicencePosition().getLicenceTransaction().getRegulatorReference();
    };
  }
}
