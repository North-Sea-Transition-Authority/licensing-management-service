package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecision;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionResponse;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;

@ExtendWith(MockitoExtension.class)
class DecisionResponseSummarySectionServiceTest {

  @Mock
  private RecordOfDecisionService recordOfDecisionService;

  @InjectMocks
  private DecisionResponseSummarySectionService decisionResponseSummarySectionService;

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
  void getSummarySection_whenTheDecisionIsAnswered_showsBothResponses() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setExtensionDecision(RecordOfDecisionResponse.GRANTED);
    recordOfDecision.setWorkProgrammeDecision(RecordOfDecisionResponse.NOT_REQUESTED);
    when(recordOfDecisionService.findByApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(recordOfDecision));

    var section = decisionResponseSummarySectionService.getSummarySection(context, null).orElseThrow();

    assertThat(section.displayOrder())
        .isEqualTo(DecisionResponseSummarySectionService.SECTION_DISPLAY_ORDER);
    assertThat(section.summaryItems())
        .extracting(SummaryItem::displayName)
        .containsExactly(DecisionResponseSummarySectionService.SECTION_NAME);
    var summaryData = (SummaryDataView) section.summaryItems().getFirst().summaryCards().getFirst().summaryData();

    assertThat(summaryData.keyValues())
        .extracting(Object::toString)
        .anyMatch(data -> data.contains(DecisionResponseSummarySectionService.DURATION_CHANGE))
        .anyMatch(data -> data.contains(RecordOfDecisionResponse.GRANTED.getDisplayName()))
        .anyMatch(data -> data.contains(DecisionResponseSummarySectionService.WORK_PROGRAMME_CHANGE))
        .anyMatch(data -> data.contains(RecordOfDecisionResponse.NOT_REQUESTED.getDisplayName()));
  }

  @Test
  void getSummarySection_whenTheDecisionIsNotAnswered_showsAnEmptyCard() {
    when(recordOfDecisionService.findByApplicationDetail(applicationDetail)).thenReturn(Optional.empty());

    var section = decisionResponseSummarySectionService.getSummarySection(context, null).orElseThrow();

    assertThat(section.summaryItems().getFirst().summaryCards())
        .extracting(SummaryCard::summaryCardType)
        .containsExactly(SummaryCardType.EMPTY_SUMMARY);
  }
}
