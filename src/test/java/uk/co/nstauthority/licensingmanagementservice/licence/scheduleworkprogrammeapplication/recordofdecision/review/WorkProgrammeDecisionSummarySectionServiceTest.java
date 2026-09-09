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
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionWorkProgramme;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionWorkProgrammeLicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionWorkProgrammeRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.WorkProgrammeAmendmentDecision;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeDecisionSummarySectionServiceTest {

  @Mock
  private RecordOfDecisionService recordOfDecisionService;

  @Mock
  private RecordOfDecisionWorkProgrammeRepository recordOfDecisionWorkProgrammeRepository;

  @Mock
  private RecordOfDecisionWorkProgrammeLicenceRepository recordOfDecisionWorkProgrammeLicenceRepository;

  @InjectMocks
  private WorkProgrammeDecisionSummarySectionService workProgrammeDecisionSummarySectionService;

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
  void getSummarySection_whenTheWorkProgrammeAmendmentIsNotApproved_returnsEmptyOptional() {
    when(recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail)).thenReturn(false);

    assertThat(workProgrammeDecisionSummarySectionService.getSummarySection(context, null)).isEmpty();
  }

  @Test
  void getSummarySection_showsOneCardPerRecordedDecision() {
    var waived = workProgrammeWith("Acquire 3D seismic data", WorkProgrammeAmendmentDecision.WAIVE);
    var amended = workProgrammeWith("Drill well to 3,000m", WorkProgrammeAmendmentDecision.AMEND);
    mockApproved();
    when(recordOfDecisionWorkProgrammeRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(waived, amended));
    when(recordOfDecisionWorkProgrammeLicenceRepository.findAllByRecordOfDecisionWorkProgramme(waived))
        .thenReturn(List.of());
    when(recordOfDecisionWorkProgrammeLicenceRepository.findAllByRecordOfDecisionWorkProgramme(amended))
        .thenReturn(List.of());

    var section = workProgrammeDecisionSummarySectionService.getSummarySection(context, null).orElseThrow();

    assertThat(section.displayOrder())
        .isEqualTo(WorkProgrammeDecisionSummarySectionService.SECTION_DISPLAY_ORDER);
    assertThat(section.summaryItems().getFirst().summaryCards())
        .extracting(SummaryCard::displayName)
        .containsExactly("Acquire 3D seismic data", "Drill well to 3,000m");
  }

  @Test
  void getSummarySection_whenTheDurationIsAmended_showsTheAmendedDuration() {
    var amended = workProgrammeWith("Drill well to 3,000m", WorkProgrammeAmendmentDecision.AMEND);
    amended.setAmendDuration(true);
    amended.setAmendedDuration(new ThreeFieldDuration(0, 6, 0));
    mockApproved();
    when(recordOfDecisionWorkProgrammeRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(amended));
    when(recordOfDecisionWorkProgrammeLicenceRepository.findAllByRecordOfDecisionWorkProgramme(amended))
        .thenReturn(List.of());

    var section = workProgrammeDecisionSummarySectionService.getSummarySection(context, null).orElseThrow();
    var summaryData = (SummaryDataView) section.summaryItems().getFirst().summaryCards().getFirst().summaryData();

    assertThat(summaryData.keyValues())
        .extracting(Object::toString)
        .anyMatch(data -> data.contains(WorkProgrammeDecisionSummarySectionService.AMENDED_DURATION))
        .anyMatch(data -> data.contains("6 months"));
  }

  @Test
  void getSummarySection_whenNoDecisionIsRecorded_showsAnEmptyCard() {
    mockApproved();
    when(recordOfDecisionWorkProgrammeRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of());

    var section = workProgrammeDecisionSummarySectionService.getSummarySection(context, null).orElseThrow();

    assertThat(section.summaryItems().getFirst().summaryCards())
        .extracting(SummaryCard::summaryCardType)
        .containsExactly(SummaryCardType.EMPTY_SUMMARY);
  }

  private void mockApproved() {
    when(recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail)).thenReturn(true);
  }

  private RecordOfDecisionWorkProgramme workProgrammeWith(
      String description,
      WorkProgrammeAmendmentDecision decision
  ) {
    var activity = new WorkProgrammeActivity();
    activity.setDescription(description);

    var workProgramme = new RecordOfDecisionWorkProgramme();
    workProgramme.setWorkProgrammeActivity(activity);
    workProgramme.setDecision(decision);
    return workProgramme;
  }
}
