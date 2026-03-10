package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence/continuation-application/{licenceContinuationApplicationDetailId}/review-and-submit")
@ContinuationApplicationHasStatus(value = LicenceContinuationApplicationStatus.DRAFT)
@InvokingUserCanAccessContinuationApplication
public class ContinuationApplicationReviewAndSubmitController {

  private final LicenceService licenceService;
  private final LicenceContinuationService licenceContinuationService;
  private final ContinuationSummarySectionService continuationSummarySectionService;

  public ContinuationApplicationReviewAndSubmitController(
      LicenceService licenceService,
      LicenceContinuationService licenceContinuationService,
      ContinuationSummarySectionService continuationSummarySectionService
  ) {
    this.licenceService = licenceService;
    this.licenceContinuationService = licenceContinuationService;
    this.continuationSummarySectionService = continuationSummarySectionService;
  }

  @GetMapping
  public ModelAndView getReviewAndSubmit(
      @PathVariable UUID licenceContinuationApplicationDetailId,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return getReviewAndSubmitModelAndView(licenceContinuationApplicationDetail);
  }

  private ModelAndView getReviewAndSubmitModelAndView(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return new ModelAndView("lms/licence/continuation/reviewAndSubmit")
        .addObject(
            "cancelUrl",
            ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(
                licenceContinuationApplicationDetail.getId(),
                null,
                null
            ))
        )
        .addObject(
            "pageCaption",
            licenceService.getLicencePageCaption(licenceContinuationService.getLicenceFromContinuationApplicationDetail(
                licenceContinuationApplicationDetail
            ))
        )
        .addObject(
            "summarySections",
            continuationSummarySectionService.getSummarySections(licenceContinuationApplicationDetail, null
            )
        )
        .addObject(
            "accordionId",
            licenceContinuationApplicationDetail.getId()
        );
  }
}