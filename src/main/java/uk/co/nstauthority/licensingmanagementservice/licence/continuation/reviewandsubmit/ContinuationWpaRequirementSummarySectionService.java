package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaRequirementRequest;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationWpaRequirementService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class ContinuationWpaRequirementSummarySectionService
    implements SummarySectionService<LicenceContinuationApplicationDetail> {

  public static final String SECTION_NAME = "Work programme activities requirement";
  public static final int SECTION_DISPLAY_ORDER = 20;
  private final LicenceContinuationWpaRequirementService licenceContinuationWpaRequirementService;
  private final LicenceContinuationService licenceContinuationService;
  private final WorkProgrammeActivityService workProgrammeActivityService;

  public ContinuationWpaRequirementSummarySectionService(
      LicenceContinuationWpaRequirementService licenceContinuationWpaRequirementService,
      LicenceContinuationService licenceContinuationService,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.licenceContinuationWpaRequirementService = licenceContinuationWpaRequirementService;
    this.licenceContinuationService = licenceContinuationService;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {
    var scheduleDetail = licenceContinuationService.getScheduleDetailFromApplicationDetail(
        licenceContinuationApplicationDetail
    );

    if (!workProgrammeActivityService.hasCurrentWorkProgrammeActivities(scheduleDetail)) {
      return Optional.empty();
    }

    var wpaRequirementSummaryItem = getWpaRequirementSummaryItem(licenceContinuationApplicationDetail);
    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, List.of(wpaRequirementSummaryItem));
    return Optional.of(summarySection);
  }

  private SummaryItem getWpaRequirementSummaryItem(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var wpaRequirementRequest = licenceContinuationWpaRequirementService.getWorkProgrammeActivitiesRequirementRequest(
        licenceContinuationApplicationDetail
    );
    List<SummaryCard> summaryCards = new ArrayList<>();

    if (wpaRequirementRequest.isPresent()) {
      var wpaRequirementSummaryCard = buildWpaRequirementRequestSummaryCard(wpaRequirementRequest.get());
      summaryCards.add(wpaRequirementSummaryCard);
    }

    return SummaryItem.withCards(
        SECTION_NAME,
        summaryCards
    );
  }

  private SummaryCard buildWpaRequirementRequestSummaryCard(
      LicenceContinuationWpaRequirementRequest licenceContinuationWpaRequirementRequest
  ) {
    var wpaRequestBuilder = SummaryDataView.newBuilder()
        .addStringValue(
            "Work programme activities completed and evidenced",
            licenceContinuationWpaRequirementRequest.getWorkProgrammeActivitiesCompletionStatus()
        );

    if (BooleanUtils.isFalse(licenceContinuationWpaRequirementRequest.getWorkProgrammeActivitiesCompletionStatus())) {
      wpaRequestBuilder.addStringValue(
          "Actions to complete work programme activities",
          licenceContinuationWpaRequirementRequest.getActionsToCompleteWorkProgrammeActivities()
      );
    } else if (BooleanUtils.isTrue(licenceContinuationWpaRequirementRequest.getWorkProgrammeActivitiesCompletionStatus())) {
      wpaRequestBuilder.addStringValue(
          "Further information",
          licenceContinuationWpaRequirementRequest.getFurtherInformation()
      );
    }

    return SummaryCard.simpleSummaryCardWithHeading(
        "Completed and evidenced",
        wpaRequestBuilder.build()
    );
  }
}