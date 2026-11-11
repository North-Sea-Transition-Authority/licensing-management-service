package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordDurationChangeSummaryView;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordDurationChangesService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionExtensionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@ExtendWith(MockitoExtension.class)
class DurationChangeDecisionSummarySectionServiceTest {

  @Mock
  private RecordOfDecisionService recordOfDecisionService;

  @Mock
  private RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;

  @Mock
  private RecordDurationChangesService recordDurationChangesService;

  @InjectMocks
  private DurationChangeDecisionSummarySectionService durationChangeDecisionSummarySectionService;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;
  private RecordOfDecisionSummaryContext context;

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    context = new RecordOfDecisionSummaryContext(applicationDetail);
  }

  @Test
  void getSummarySection_whenTheExtensionIsNotApproved_thenReturnsEmptyOptional() {
    when(recordOfDecisionService.isExtensionApproved(applicationDetail)).thenReturn(false);

    var result = durationChangeDecisionSummarySectionService.getSummarySection(context, null);

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenNoExtensionIsRecorded_thenShowsAnEmptyCard() {
    when(recordOfDecisionService.isExtensionApproved(applicationDetail)).thenReturn(true);
    when(recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(false);

    var result = durationChangeDecisionSummarySectionService.getSummarySection(context, null);

    var expected = new SummarySection(
        DurationChangeDecisionSummarySectionService.SECTION_DISPLAY_ORDER,
        List.of(SummaryItem.withCard(
            DurationChangeDecisionSummarySectionService.SECTION_NAME,
            SummaryCard.emptySummaryCard())));
    assertThat(result).contains(expected);
  }

  @Test
  void getSummarySection_whenAnExtensionIsRecorded_thenShowsACardForEachTermAndPhase() {
    when(recordOfDecisionService.isExtensionApproved(applicationDetail)).thenReturn(true);
    when(recordOfDecisionExtensionRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(true);
    var extendedView = new RecordDurationChangeSummaryView(
        "Initial term", false, "4 years", "1 January 2030", "Extended by 1 year", "5 years", "1 January 2031");
    var reducedView = new RecordDurationChangeSummaryView(
        "Phase B", true, "2 years", "1 January 2032", "Reduced by 1 year", "1 year", "1 January 2032");
    when(recordDurationChangesService.getSummaryViews(applicationDetail))
        .thenReturn(List.of(extendedView, reducedView));

    var result = durationChangeDecisionSummarySectionService.getSummarySection(context, null);

    var expected = new SummarySection(
        DurationChangeDecisionSummarySectionService.SECTION_DISPLAY_ORDER,
        List.of(SummaryItem.withCards(
            DurationChangeDecisionSummarySectionService.SECTION_NAME,
            List.of(expectedCard(extendedView), expectedCard(reducedView)))));
    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  private SummaryCard expectedCard(RecordDurationChangeSummaryView view) {
    return SummaryCard.simpleSummaryCardWithHeading(
        view.displayName(),
        SummaryDataView.newBuilder()
            .addStringValue(DurationChangeDecisionSummarySectionService.CURRENT_DURATION, view.currentDuration())
            .addStringValue(DurationChangeDecisionSummarySectionService.CURRENT_END_DATE, view.currentEndDate())
            .addStringValue(DurationChangeDecisionSummarySectionService.CHANGE, view.change())
            .addStringValue(DurationChangeDecisionSummarySectionService.NEW_DURATION, view.newDuration())
            .addStringValue(DurationChangeDecisionSummarySectionService.NEW_END_DATE, view.newEndDate())
            .build());
  }
}
