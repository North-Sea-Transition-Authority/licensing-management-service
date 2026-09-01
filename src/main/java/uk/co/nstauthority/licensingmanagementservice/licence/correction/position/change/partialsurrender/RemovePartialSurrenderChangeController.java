package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class RemovePartialSurrenderChangeController {

  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this partial surrender?";
  private static final String REMOVE_PRIMARY_BUTTON_TEXT = "Remove partial surrender";
  private static final String CONFIRMATION_TEMPLATE =
      "lms/licence/correction/change/partialSurrender/removePartialSurrenderChange";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionService licencePositionService;

  public RemovePartialSurrenderChangeController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionService licencePositionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionService = licencePositionService;
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/remove-partial-surrender")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  public ModelAndView renderRemoveExecutedPartialSurrender(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);

    var executedSurrender = partialSurrenderCorrectionService.getLiveSurrenderOrThrow(changeId);
    var blockRows = partialSurrenderCorrectionService.getBlockRows(executedSurrender);

    var surrenderDate = Objects.requireNonNullElseGet(
        executedSurrender.surrenderDate(),
        () -> licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition));

    var cancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), licencePosition.getId(), null));

    return confirmationModelAndView(surrenderDate, blockRows, cancelUrl);
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/remove-partial-surrender")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  public ModelAndView removePartialSurrender(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);

    partialSurrenderCorrectionService.removeExistingPartialSurrender(licencePosition, correction, changeId);

    NotificationBanner.newSuccessBannerWithHeader("Partial surrender removed", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), licencePositionId, null));
  }

  private static ModelAndView confirmationModelAndView(
      @Nullable LocalDate surrenderDate,
      List<PartialSurrenderChangeView.BlockRow> blockRows,
      String cancelUrl
  ) {
    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", REMOVE_PAGE_TITLE)
        .addObject("primaryButtonText", REMOVE_PRIMARY_BUTTON_TEXT)
        .addObject("surrenderDate", surrenderDate == null ? null : DateUtil.formatLongDate(surrenderDate))
        .addObject("blockRows", blockRows)
        .addObject("cancelUrl", cancelUrl);
  }
}
