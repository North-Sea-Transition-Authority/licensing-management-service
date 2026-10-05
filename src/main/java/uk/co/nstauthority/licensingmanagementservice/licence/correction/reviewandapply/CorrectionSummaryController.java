package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserHasCorrectorRoleForCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionDetailsViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/summary")
@InvokingUserHasCorrectorRoleForCorrection
@CorrectionHasStatus({LicenceCorrectionStatus.IN_PROGRESS, LicenceCorrectionStatus.COMPLETE})
public class CorrectionSummaryController {

  private final CorrectionDetailsViewService correctionDetailsViewService;
  private final LicenceService licenceService;
  private final CorrectionSummaryService correctionSummaryService;
  private final TabbedLicencePageService tabbedLicencePageService;

  public CorrectionSummaryController(
      CorrectionDetailsViewService correctionDetailsViewService,
      LicenceService licenceService,
      CorrectionSummaryService correctionSummaryService,
      TabbedLicencePageService tabbedLicencePageService
  ) {
    this.correctionDetailsViewService = correctionDetailsViewService;
    this.licenceService = licenceService;
    this.correctionSummaryService = correctionSummaryService;
    this.tabbedLicencePageService = tabbedLicencePageService;
  }

  @GetMapping
  public ModelAndView renderCorrectionSummary(LicenceCorrection licenceCorrection) {
    return new ModelAndView("lms/licence/correction/reviewandapply/correctionSummary")
        .addObject("pageCaption", licenceService.getLicencePageCaption(licenceCorrection.getLicence()))
        .addObject("pageTitle", "Correction summary")
        .addObject("correction", licenceCorrection)
        .addObject("correctionDetails", correctionDetailsViewService.getDetailsView(licenceCorrection))
        .addObject("positions", correctionSummaryService.getSummaryPositions(licenceCorrection))
        .addObject("backLinkUrl", tabbedLicencePageService.getDefaultTabUrl(licenceCorrection.getLicence()));
  }
}