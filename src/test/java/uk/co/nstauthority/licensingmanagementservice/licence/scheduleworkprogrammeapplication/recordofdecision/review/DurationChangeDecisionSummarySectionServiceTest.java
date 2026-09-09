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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionExtensionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionReduction;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionReductionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ExtendWith(MockitoExtension.class)
class DurationChangeDecisionSummarySectionServiceTest {

  @Mock
  private RecordOfDecisionService recordOfDecisionService;

  @Mock
  private RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;

  @Mock
  private RecordOfDecisionReductionRepository recordOfDecisionReductionRepository;

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
  void getSummarySection_whenTheExtensionIsNotApproved_returnsEmptyOptional() {
    when(recordOfDecisionService.isExtensionApproved(applicationDetail)).thenReturn(false);

    assertThat(durationChangeDecisionSummarySectionService.getSummarySection(context, null)).isEmpty();
  }

  @Test
  void getSummarySection_showsAnExtensionsCardAndAReductionsCard() {
    mockApproved();
    mockRecordedChanges(
        List.of(extensionFor(term(TermType.INITIAL), new ThreeFieldDuration(1, 0, 0))),
        List.of(reductionFor(term(TermType.THIRD), new ThreeFieldDuration(1, 0, 0))));

    var section = durationChangeDecisionSummarySectionService.getSummarySection(context, null).orElseThrow();

    assertThat(section.displayOrder())
        .isEqualTo(DurationChangeDecisionSummarySectionService.SECTION_DISPLAY_ORDER);
    assertThat(section.summaryItems().getFirst().displayName())
        .isEqualTo(DurationChangeDecisionSummarySectionService.SECTION_NAME);
    assertThat(section.summaryItems().getFirst().summaryCards())
        .extracting(SummaryCard::displayName)
        .containsExactly(
            DurationChangeDecisionSummarySectionService.EXTENSIONS_CARD_NAME,
            DurationChangeDecisionSummarySectionService.REDUCTIONS_CARD_NAME);
  }

  @Test
  void getSummarySection_showsEachRecordedDurationInsideItsOwnCard() {
    mockApproved();
    mockRecordedChanges(
        List.of(extensionFor(term(TermType.INITIAL), new ThreeFieldDuration(1, 0, 0))),
        List.of(reductionFor(term(TermType.THIRD), new ThreeFieldDuration(0, 6, 0))));

    var section = durationChangeDecisionSummarySectionService.getSummarySection(context, null).orElseThrow();
    var cards = section.summaryItems().getFirst().summaryCards();

    assertThat(((SummaryDataView) cards.getFirst().summaryData()).keyValues())
        .extracting(Object::toString)
        .anyMatch(data -> data.contains(DurationChangeDecisionSummarySectionService.EXTENSION_DURATION
            .formatted(TermType.INITIAL.getDisplayName())))
        .anyMatch(data -> data.contains("1 year"));

    assertThat(((SummaryDataView) cards.getLast().summaryData()).keyValues())
        .extracting(Object::toString)
        .anyMatch(data -> data.contains(DurationChangeDecisionSummarySectionService.REDUCTION_DURATION
            .formatted(TermType.THIRD.getDisplayName())))
        .anyMatch(data -> data.contains("6 months"));
  }

  @Test
  void getSummarySection_whenOnlyExtensionsAreRecorded_showsOnlyTheExtensionsCard() {
    mockApproved();
    mockRecordedChanges(
        List.of(extensionFor(term(TermType.INITIAL), new ThreeFieldDuration(1, 0, 0))),
        List.of());

    var section = durationChangeDecisionSummarySectionService.getSummarySection(context, null).orElseThrow();

    assertThat(section.summaryItems().getFirst().summaryCards())
        .extracting(SummaryCard::displayName)
        .containsExactly(DurationChangeDecisionSummarySectionService.EXTENSIONS_CARD_NAME);
  }

  @Test
  void getSummarySection_whenNothingIsRecorded_showsAnEmptyCard() {
    mockApproved();
    mockRecordedChanges(List.of(), List.of());

    var section = durationChangeDecisionSummarySectionService.getSummarySection(context, null).orElseThrow();

    assertThat(section.summaryItems().getFirst().summaryCards())
        .extracting(SummaryCard::summaryCardType)
        .containsExactly(SummaryCardType.EMPTY_SUMMARY);
  }

  private void mockApproved() {
    when(recordOfDecisionService.isExtensionApproved(applicationDetail)).thenReturn(true);
  }

  private void mockRecordedChanges(
      List<RecordOfDecisionExtension> extensions,
      List<RecordOfDecisionReduction> reductions
  ) {
    when(recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(extensions);
    when(recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(reductions);
  }

  private LicenceScheduleTerm term(TermType termType) {
    return LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(termType)
        .build();
  }

  private RecordOfDecisionExtension extensionFor(LicenceScheduleTerm term, ThreeFieldDuration duration) {
    var extension = new RecordOfDecisionExtension();
    extension.setLicenceScheduleTerm(term);
    extension.setExtensionDuration(duration);
    return extension;
  }

  private RecordOfDecisionReduction reductionFor(LicenceScheduleTerm term, ThreeFieldDuration duration) {
    var reduction = new RecordOfDecisionReduction();
    reduction.setLicenceScheduleTerm(term);
    reduction.setReductionDuration(duration);
    return reduction;
  }
}
