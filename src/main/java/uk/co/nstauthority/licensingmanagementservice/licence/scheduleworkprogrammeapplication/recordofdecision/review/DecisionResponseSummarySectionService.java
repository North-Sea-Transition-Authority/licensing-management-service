package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.review;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionResponse;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class DecisionResponseSummarySectionService implements SummarySectionService<RecordOfDecisionSummaryContext> {

  public static final String SECTION_NAME = "What is the decision?";
  public static final String DURATION_CHANGE = "Is there a change to a phase/term duration?";
  public static final String WORK_PROGRAMME_CHANGE = "Is there a change to a work programme activity?";
  public static final int SECTION_DISPLAY_ORDER = 10;

  private final RecordOfDecisionService recordOfDecisionService;

  public DecisionResponseSummarySectionService(RecordOfDecisionService recordOfDecisionService) {
    this.recordOfDecisionService = recordOfDecisionService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      RecordOfDecisionSummaryContext context,
      ServiceUserDetail user
  ) {
    var recordOfDecision = recordOfDecisionService.findByApplicationDetail(context.applicationDetail());

    if (recordOfDecision.isEmpty()) {
      return Optional.of(new SummarySection(
          SECTION_DISPLAY_ORDER,
          List.of(SummaryItem.withCard(SECTION_NAME, SummaryCard.emptySummaryCard()))));
    }

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue(DURATION_CHANGE, displayName(recordOfDecision.get().getExtensionDecision()))
        .addStringValue(WORK_PROGRAMME_CHANGE, displayName(recordOfDecision.get().getWorkProgrammeDecision()))
        .build();

    return Optional.of(new SummarySection(
        SECTION_DISPLAY_ORDER,
        List.of(SummaryItem.withCard(SECTION_NAME, SummaryCard.simpleSummaryCard(summaryDataView)))));
  }

  private String displayName(RecordOfDecisionResponse response) {
    return response == null ? "" : response.getDisplayName();
  }
}
