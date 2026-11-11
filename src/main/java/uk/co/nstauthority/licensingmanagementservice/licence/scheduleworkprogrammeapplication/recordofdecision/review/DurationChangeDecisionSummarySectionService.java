package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.review;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordDurationChangeSummaryView;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordDurationChangesService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionExtensionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class DurationChangeDecisionSummarySectionService
    implements SummarySectionService<RecordOfDecisionSummaryContext> {

  public static final String SECTION_NAME = "Term and phase durations";
  public static final String CURRENT_DURATION = "Current duration";
  public static final String CURRENT_END_DATE = "Current end date";
  public static final String CHANGE = "Change";
  public static final String NEW_DURATION = "New duration";
  public static final String NEW_END_DATE = "New end date";
  public static final int SECTION_DISPLAY_ORDER = 20;

  private final RecordOfDecisionService recordOfDecisionService;
  private final RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;
  private final RecordDurationChangesService recordDurationChangesService;

  public DurationChangeDecisionSummarySectionService(
      RecordOfDecisionService recordOfDecisionService,
      RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository,
      RecordDurationChangesService recordDurationChangesService
  ) {
    this.recordOfDecisionService = recordOfDecisionService;
    this.recordOfDecisionExtensionRepository = recordOfDecisionExtensionRepository;
    this.recordDurationChangesService = recordDurationChangesService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      RecordOfDecisionSummaryContext context,
      ServiceUserDetail user
  ) {
    var applicationDetail = context.applicationDetail();

    if (!recordOfDecisionService.isExtensionApproved(applicationDetail)) {
      return Optional.empty();
    }

    if (!recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail)) {
      return Optional.of(new SummarySection(
          SECTION_DISPLAY_ORDER,
          List.of(SummaryItem.withCard(SECTION_NAME, SummaryCard.emptySummaryCard()))));
    }

    var cards = recordDurationChangesService.getSummaryViews(applicationDetail)
        .stream()
        .map(this::toSummaryCard)
        .toList();

    return Optional.of(new SummarySection(
        SECTION_DISPLAY_ORDER,
        List.of(SummaryItem.withCards(SECTION_NAME, cards))));
  }

  private SummaryCard toSummaryCard(RecordDurationChangeSummaryView view) {
    var summaryData = SummaryDataView.newBuilder()
        .addStringValue(CURRENT_DURATION, view.currentDuration())
        .addStringValue(CURRENT_END_DATE, view.currentEndDate())
        .addStringValue(CHANGE, view.change())
        .addStringValue(NEW_DURATION, view.newDuration())
        .addStringValue(NEW_END_DATE, view.newEndDate())
        .build();

    return SummaryCard.simpleSummaryCardWithHeading(view.displayName(), summaryData);
  }
}
