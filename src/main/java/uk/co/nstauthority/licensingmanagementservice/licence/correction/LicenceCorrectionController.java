package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.AddLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.update.UpdateCorrectionGeneralDetailsController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping("/licence-corrections")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
@LogWorkAreaItemView(
    itemType = WorkAreaDataItemType.LICENCE_CORRECTION,
    pathVariable = "correctionId"
)
public class LicenceCorrectionController {

  private final LicencePositionService licencePositionService;
  private final LicencePositionViewService licencePositionViewService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicenceService licenceService;
  private final EnergyPortalUserService energyPortalUserService;

  public LicenceCorrectionController(
      LicencePositionService licencePositionService,
      LicencePositionViewService licencePositionViewService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicenceService licenceService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionViewService = licencePositionViewService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licenceService = licenceService;
    this.energyPortalUserService = energyPortalUserService;
  }

  @GetMapping("/{correctionId}")
  public ModelAndView renderCorrection(
      @PathVariable UUID correctionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection
  ) {
    var licence = licenceCorrection.getLicence();
    var executedLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    var addedPositions = licencePositionCorrectionService.getAddedLicencePositionCorrections(licenceCorrection);

    if (executedLicencePositions.isEmpty() && addedPositions.isEmpty()) {
      return licencePositionsModelAndView(licenceCorrection, LicencePositionPageView.empty());
    }

    if (!executedLicencePositions.isEmpty()) {
      return ReverseRouter.redirect(on(this.getClass()).renderLicencePosition(
          correctionId, executedLicencePositions.getLast().getId(), licenceCorrection));
    }

    return ReverseRouter.redirect(on(this.getClass()).renderAddedPosition(
        correctionId, addedPositions.getLast().getId(), licenceCorrection));
  }

  @GetMapping("/{correctionId}/{licencePositionId}")
  public ModelAndView renderLicencePosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection
  ) {
    var licence = licenceCorrection.getLicence();
    var licencePosition = licencePositionService.getPositionForLicence(licence, licencePositionId);
    var licencePositionPageView = licencePositionViewService.getCorrectionPositionPageView(licenceCorrection, licencePosition);

    return licencePositionsModelAndView(licenceCorrection, licencePositionPageView);
  }

  @GetMapping("/{correctionId}/added-positions/{licencePositionCorrectionId}")
  public ModelAndView renderAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, licenceCorrection);
    var licencePositionPageView = licencePositionViewService
        .getCorrectionAddedPositionPageView(licenceCorrection, positionCorrection);

    return licencePositionsModelAndView(licenceCorrection, licencePositionPageView);
  }

  private ModelAndView licencePositionsModelAndView(
      LicenceCorrection licenceCorrection,
      LicencePositionPageView licencePositionPageView
  ) {
    var licence =  licenceCorrection.getLicence();
    var allocatedToUserDetail = energyPortalUserService.getByWuaId(
        WebUserAccountId.from(licenceCorrection.getAllocatedToWuaId()),
        "Get correction allocated to user details"
    );

    return new ModelAndView("lms/licence/correction/viewCorrection")
        .addObject("pageTitle", "%s - licence correction".formatted(licence.getLicenceReference()))
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("licencePositionPageView", licencePositionPageView)
        .addObject("correction", licenceCorrection)
        .addObject("allocatedToUser", allocatedToUserDetail.displayName())
        .addObject("addPositionUrl",
            ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
                .renderAddLicencePositionCorrection(licenceCorrection.getId(), null)))
        .addObject("updateGeneralDetailsUrl",
            ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
                .renderUpdateGeneralDetails(licenceCorrection.getId(), null)))
        .addObject("canUpdateGeneralDetails",
            LicenceCorrectionStatus.IN_PROGRESS.equals(licenceCorrection.getStatus()))
        .addObject("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
            .renderCancelCorrection(licenceCorrection.getId(), null)));
  }

}