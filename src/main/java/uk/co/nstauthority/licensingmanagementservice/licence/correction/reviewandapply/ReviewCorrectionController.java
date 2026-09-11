package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/review")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
public class ReviewCorrectionController {

  private final EnergyPortalUserService energyPortalUserService;
  private final LicenceService licenceService;
  private final CorrectionReviewService correctionReviewService;

  public ReviewCorrectionController(EnergyPortalUserService energyPortalUserService, LicenceService licenceService,
                                    CorrectionReviewService correctionReviewService) {
    this.energyPortalUserService = energyPortalUserService;
    this.licenceService = licenceService;
    this.correctionReviewService = correctionReviewService;
  }

  @GetMapping
  public ModelAndView renderReviewCorrection(
      @PathVariable UUID correctionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection
  ) {
    var positions = correctionReviewService.getReviewPositions(licenceCorrection);
    return licencePositionsModelAndView(correctionId, licenceCorrection)
        .addObject("positions", positions);
  }

  private ModelAndView licencePositionsModelAndView(
      UUID correctionId,
      LicenceCorrection licenceCorrection
  ) {
    var allocatedToUserDetail = energyPortalUserService.getByWuaId(
        WebUserAccountId.from(licenceCorrection.getAllocatedToWuaId()),
        "Get correction allocated to user details"
    );

    return new ModelAndView("lms/licence/correction/reviewandapply/reviewCorrection")
        .addObject("pageCaption", licenceService.getLicencePageCaption(licenceCorrection.getLicence()))
        .addObject("pageTitle", "Do you want to apply this correction?")
        .addObject("correction", licenceCorrection)
        .addObject("allocatedToUser", allocatedToUserDetail.displayName())
        .addObject("createdDate", DateUtil.formatLongDate(licenceCorrection.getCreatedInstant()))
        .addObject("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correctionId, null)));
  }
}