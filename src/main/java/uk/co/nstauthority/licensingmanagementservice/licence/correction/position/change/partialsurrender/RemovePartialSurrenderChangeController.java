package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class RemovePartialSurrenderChangeController {

  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this partial surrender?";
  private static final String REMOVE_PRIMARY_BUTTON_TEXT = "Remove partial surrender";
  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this partial surrender?";
  private static final String UNDO_PRIMARY_BUTTON_TEXT = "Undo partial surrender";
  private static final String CONFIRMATION_TEMPLATE =
      "lms/licence/correction/change/partialSurrender/removePartialSurrenderChange";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  public RemovePartialSurrenderChangeController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/remove-partial-surrender")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderRemoveExecutedPartialSurrender(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change
  ) {
    var executedSurrender = partialSurrenderCorrectionService.getLiveSurrenderOrThrow(change.getId().toString());
    var blockRows = partialSurrenderCorrectionService.getBlockRows(executedSurrender);

    var surrenderDate = Objects.requireNonNullElseGet(
        executedSurrender.surrenderDate(),
        () -> licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition));

    var cancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));

    return confirmationModelAndView(
        surrenderDate, blockRows, REMOVE_PAGE_TITLE, REMOVE_PRIMARY_BUTTON_TEXT, cancelUrl);
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/remove-partial-surrender")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView removePartialSurrender(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      RedirectAttributes redirectAttributes
  ) {
    partialSurrenderCorrectionService.removeExistingPartialSurrender(licencePosition, correction, change.getId().toString());

    NotificationBanner.newSuccessBannerWithHeader("Partial surrender removed", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  @GetMapping("/change/{changeId}/undo-partial-surrender")
  public ModelAndView renderUndoPartialSurrender(
      LicenceCorrection correction,
      @PathVariable String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    var stagedSurrender = partialSurrenderCorrectionService
        .getStagedPartialSurrenderOrThrow(positionCorrection, changeId);
    var blockRows = partialSurrenderCorrectionService.getBlockRows(stagedSurrender);
    var surrenderDate = Objects.requireNonNullElseGet(
        stagedSurrender.surrenderDate(),
        () -> licencePositionCorrectionService.resolveEffectiveDate(positionCorrection));

    return confirmationModelAndView(
        surrenderDate,
        blockRows,
        UNDO_PAGE_TITLE,
        UNDO_PRIMARY_BUTTON_TEXT,
        positionPageRoute(correction, positionCorrection));
  }

  @PostMapping("/change/{changeId}/undo-partial-surrender")
  public ModelAndView undoPartialSurrender(
      LicenceCorrection correction,
      @PathVariable String changeId,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    partialSurrenderCorrectionService.undoPartialSurrenderChange(correction, changeId);

    NotificationBanner.newSuccessBannerWithHeader("Partial surrender undone", redirectAttributes);

    return ReverseRouter.redirectToUrl(positionPageRoute(correction, positionCorrection));
  }

  private static ModelAndView confirmationModelAndView(
      @Nullable LocalDate surrenderDate,
      List<PartialSurrenderChangeView.BlockRow> blockRows,
      String pageTitle,
      String primaryButtonText,
      String cancelUrl
  ) {
    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", pageTitle)
        .addObject("primaryButtonText", primaryButtonText)
        .addObject("surrenderDate", surrenderDate == null ? null : DateUtil.formatLongDate(surrenderDate))
        .addObject("blockRows", blockRows)
        .addObject("cancelUrl", cancelUrl);
  }

  private static String positionPageRoute(
      LicenceCorrection correction,
      LicencePositionCorrection positionCorrection
  ) {
    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.route(on(LicenceCorrectionController.class).renderAddedPosition(correction, positionCorrection));
    }

    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition()));
  }
}
