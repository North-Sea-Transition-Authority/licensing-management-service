package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;

@ExtendWith(MockitoExtension.class)
class TermOrPhaseEndDeadlineServiceTest {

  private static final LocalDate INITIAL_TERM_END = LocalDate.of(2027, Month.DECEMBER, 31);
  private static final LocalDate SECOND_TERM_END = LocalDate.of(2031, Month.DECEMBER, 31);
  private static final LocalDate THIRD_TERM_END = LocalDate.of(2049, Month.DECEMBER, 31);
  private static final LocalDate PHASE_A_END = LocalDate.of(2025, Month.DECEMBER, 31);

  private static final LocalDate INITIAL_TERM_NOTICE_DATE = LocalDate.of(2027, Month.JUNE, 30);
  private static final LocalDate INITIAL_TERM_WINDOW_END = LocalDate.of(2028, Month.JANUARY, 2);

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private Clock clock;

  @InjectMocks
  private TermOrPhaseEndDeadlineService termOrPhaseEndDeadlineService;

  private Licence licence;
  private LicenceScheduleDetail scheduleDetail;
  private LicenceScheduleTerm initialTerm;
  private LicenceScheduleTerm secondTerm;
  private LicenceScheduleTerm thirdTerm;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setId(UUID.randomUUID());
    licenceSchedule.setLicence(licence);

    scheduleDetail = new LicenceScheduleDetail();
    scheduleDetail.setId(UUID.randomUUID());
    scheduleDetail.setLicenceSchedule(licenceSchedule);
    scheduleDetail.setStatus(LicenceScheduleDetailStatus.ACTIVE);

    initialTerm = term(TermType.INITIAL, LocalDate.of(2024, Month.JANUARY, 1), INITIAL_TERM_END);
    secondTerm = term(TermType.SECOND, LocalDate.of(2028, Month.JANUARY, 1), SECOND_TERM_END);
    thirdTerm = term(TermType.THIRD, LocalDate.of(2032, Month.JANUARY, 1), THIRD_TERM_END);
  }

  @Test
  void getDeadlinesDueReminder_whenNothingIsInTheNoticeWindow_thenNothingIsDue() {
    mockWindow(INITIAL_TERM_NOTICE_DATE, INITIAL_TERM_WINDOW_END, List.of(), List.of());

    assertThat(termOrPhaseEndDeadlineService.getDeadlinesDueReminder()).isEmpty();

    verify(licenceScheduleTermService, never()).getTermsByLicenceScheduleDetails(Set.of(scheduleDetail));
  }

  @Test
  void getDeadlinesDueReminder_onTheNoticeDate_thenTheTermIsDue() {
    mockWindow(INITIAL_TERM_NOTICE_DATE, INITIAL_TERM_WINDOW_END, List.of(initialTerm), List.of());
    mockAllTermsOnTheSchedule(List.of(initialTerm, secondTerm, thirdTerm));

    var deadlines = termOrPhaseEndDeadlineService.getDeadlinesDueReminder();

    assertThat(deadlines).containsExactly(new ReminderDeadline(
        initialTerm,
        initialTerm.getOriginalEventId(),
        licence,
        INITIAL_TERM_END,
        TermType.INITIAL.getDisplayName(),
        ReminderType.TERM_OR_PHASE_END));
  }

  @Test
  void getDeadlinesDueReminder_theDayBeforeTheNoticeDate_thenNothingIsDue() {
    mockWindow(
        LocalDate.of(2027, Month.JUNE, 29),
        LocalDate.of(2028, Month.JANUARY, 1),
        List.of(initialTerm),
        List.of());
    mockAllTermsOnTheSchedule(List.of(initialTerm, secondTerm, thirdTerm));

    assertThat(termOrPhaseEndDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_whenTheTermIsTheFinalTerm_thenItIsNotDue() {
    mockWindow(
        LocalDate.of(2049, Month.JUNE, 30),
        LocalDate.of(2050, Month.JANUARY, 2),
        List.of(thirdTerm),
        List.of());
    mockAllTermsOnTheSchedule(List.of(initialTerm, secondTerm, thirdTerm));

    assertThat(termOrPhaseEndDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_whenOnlyOneTermExists_thenItIsTheFinalTermAndNotDue() {
    mockWindow(INITIAL_TERM_NOTICE_DATE, INITIAL_TERM_WINDOW_END, List.of(initialTerm), List.of());
    mockAllTermsOnTheSchedule(List.of(initialTerm));

    assertThat(termOrPhaseEndDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_whenAPhaseEndsOnItsTermEndDate_thenOnlyTheTermIsDue() {
    var coincidingPhase = phase(PhaseType.PHASE_B, initialTerm, INITIAL_TERM_END);
    mockWindow(
        INITIAL_TERM_NOTICE_DATE,
        INITIAL_TERM_WINDOW_END,
        List.of(initialTerm),
        List.of(coincidingPhase));
    mockAllTermsOnTheSchedule(List.of(initialTerm, secondTerm, thirdTerm));

    var deadlines = termOrPhaseEndDeadlineService.getDeadlinesDueReminder();

    assertThat(deadlines)
        .extracting(ReminderDeadline::displayName)
        .containsExactly(TermType.INITIAL.getDisplayName());
  }

  @Test
  void getDeadlinesDueReminder_whenOnlyAPhaseIsInTheWindow_thenItIsDueWithoutReadingTheTerms() {
    var phaseA = phase(PhaseType.PHASE_A, initialTerm, PHASE_A_END);
    mockWindow(
        LocalDate.of(2025, Month.JUNE, 30),
        LocalDate.of(2026, Month.JANUARY, 2),
        List.of(),
        List.of(phaseA));

    var deadlines = termOrPhaseEndDeadlineService.getDeadlinesDueReminder();

    assertThat(deadlines).containsExactly(new ReminderDeadline(
        phaseA,
        phaseA.getOriginalEventId(),
        licence,
        PHASE_A_END,
        PhaseType.PHASE_A.getDisplayName(),
        ReminderType.TERM_OR_PHASE_END));

    verify(licenceScheduleTermService, never()).getTermsByLicenceScheduleDetails(Set.of(scheduleDetail));
  }

  @Test
  void getDeadlinesDueReminder_whenThePhaseTermHasNoEndDate_thenThePhaseIsStillDue() {
    var undatedTerm = term(TermType.INITIAL, LocalDate.of(2024, Month.JANUARY, 1), null);
    var phaseA = phase(PhaseType.PHASE_A, undatedTerm, PHASE_A_END);
    mockWindow(
        LocalDate.of(2025, Month.JUNE, 30),
        LocalDate.of(2026, Month.JANUARY, 2),
        List.of(),
        List.of(phaseA));

    var deadlines = termOrPhaseEndDeadlineService.getDeadlinesDueReminder();

    assertThat(deadlines)
        .extracting(ReminderDeadline::displayName)
        .containsExactly(PhaseType.PHASE_A.getDisplayName());
  }

  private void mockWindow(
      LocalDate today,
      LocalDate latestEndDate,
      List<LicenceScheduleTerm> windowedTerms,
      List<LicenceSchedulePhase> windowedPhases
  ) {
    when(clock.instant()).thenReturn(today.atStartOfDay().toInstant(ZoneOffset.UTC));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(licenceScheduleTermService.getTermsEndingBetweenOnActiveSchedules(today, latestEndDate))
        .thenReturn(windowedTerms);
    when(licenceSchedulePhaseService.getPhasesEndingBetweenOnActiveSchedules(today, latestEndDate))
        .thenReturn(windowedPhases);
  }

  private void mockAllTermsOnTheSchedule(List<LicenceScheduleTerm> allTerms) {
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetails(Set.of(scheduleDetail)))
        .thenReturn(allTerms);
  }

  private LicenceScheduleTerm term(TermType termType, LocalDate startDate, LocalDate endDate) {
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(UUID.randomUUID());
    term.setLicenceSchedule(scheduleDetail.getLicenceSchedule());
    term.setLicenceScheduleDetail(scheduleDetail);
    term.setTermType(termType);
    term.setStartDate(startDate);
    term.setEndDate(endDate);
    return term;
  }

  private LicenceSchedulePhase phase(PhaseType phaseType, LicenceScheduleTerm term, LocalDate endDate) {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setOriginalEventId(UUID.randomUUID());
    phase.setLicenceSchedule(scheduleDetail.getLicenceSchedule());
    phase.setLicenceScheduleDetail(scheduleDetail);
    phase.setLicenceScheduleTerm(term);
    phase.setPhaseType(phaseType);
    phase.setEndDate(endDate);
    return phase;
  }
}
