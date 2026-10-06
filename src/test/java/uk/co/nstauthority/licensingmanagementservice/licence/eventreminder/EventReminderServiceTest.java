package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;

@ExtendWith(MockitoExtension.class)
class EventReminderServiceTest {

  private static final LocalDate DEADLINE_DATE = LocalDate.of(2027, Month.DECEMBER, 31);
  private static final LocalDate LATER_DEADLINE_DATE = LocalDate.of(2028, Month.MARCH, 31);
  private static final Integer BP_ID = 181;
  private static final Integer SHELL_ID = 202;

  @Mock
  private EventReminderDeadlineSource termOrPhaseEndDeadlineSource;

  @Mock
  private EventReminderDeadlineSource licenceExpiryDeadlineSource;

  @Mock
  private EventReminderDeadlineSource workProgrammeActivityDeadlineSource;

  @Mock
  private EventReminderSuppressionService eventReminderSuppressionService;

  @Mock
  private EventReminderRecipientService eventReminderRecipientService;

  @Mock
  private EventReminderBatchService eventReminderBatchService;

  @Mock
  private LicenceReminderRepository licenceReminderRepository;

  @Mock
  private EventReminderMissingContactService eventReminderMissingContactService;

  @Captor
  private ArgumentCaptor<List<EventReminderDeadline>> deadlinesCaptor;

  private EventReminderService eventReminderService;
  private Licence licence;
  private EventReminderRecipient bpRecipient;

  @BeforeEach
  void setUp() {
    eventReminderService = new EventReminderService(
        List.of(termOrPhaseEndDeadlineSource, licenceExpiryDeadlineSource, workProgrammeActivityDeadlineSource),
        eventReminderSuppressionService,
        eventReminderRecipientService,
        eventReminderBatchService,
        licenceReminderRepository,
        eventReminderMissingContactService);
    licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();
    bpRecipient = new EventReminderRecipient(1, BP_ID, "BP Exploration Alpha Ltd", "bp@example.com");
  }

  @Test
  void sendDueReminders_whenNothingIsDue_thenNoBatchIsQueued() {
    when(termOrPhaseEndDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());
    when(workProgrammeActivityDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());
    when(licenceExpiryDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());

    var summary = eventReminderService.sendDueReminders();

    assertThat(summary).isEqualTo(new EventReminderRunSummary(0, 0, 0, 0));
    verify(eventReminderBatchService, never()).queueBatch(any(), any(), anyCollection());
    verify(eventReminderSuppressionService, never()).getSuppressedLicenceIds(anyCollection());
  }

  @Test
  void sendDueReminders_whenTheLicenceIsSuppressed_thenNoBatchIsQueued() {
    var deadline = deadline(TermType.INITIAL.getDisplayName());
    when(termOrPhaseEndDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of(deadline));
    when(workProgrammeActivityDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());
    when(licenceExpiryDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());
    when(eventReminderSuppressionService.getSuppressedLicenceIds(List.of(licence))).thenReturn(Set.of(1));

    var summary = eventReminderService.sendDueReminders();

    assertThat(summary).isEqualTo(new EventReminderRunSummary(1, 1, 0, 0));
    verify(eventReminderBatchService, never()).queueBatch(any(), any(), anyCollection());
    verify(eventReminderRecipientService, never()).getRecipientsByLicenceId(anyCollection());
  }

  @Test
  void sendDueReminders_whenTwoDeadlinesShareARecipientAndDate_thenOneBatchCoversBoth() {
    var initialTerm = deadline(TermType.INITIAL.getDisplayName());
    var secondTerm = deadline(TermType.SECOND.getDisplayName());
    mockRun(List.of(initialTerm, secondTerm), List.of(bpRecipient), List.of());

    eventReminderService.sendDueReminders();

    verify(eventReminderBatchService).queueBatch(eq(bpRecipient), eq(DEADLINE_DATE), deadlinesCaptor.capture());
    assertThat(deadlinesCaptor.getValue()).containsExactly(initialTerm, secondTerm);
  }

  @Test
  void sendDueReminders_whenATermAndAnExpiryShareARecipientAndDate_thenOneBatchCoversBoth() {
    var termDeadline = deadline(TermType.INITIAL.getDisplayName());
    var expiryDeadline = expiryDeadline(DEADLINE_DATE);
    mockRun(List.of(termDeadline), List.of(expiryDeadline), List.of(bpRecipient), List.of());

    var summary = eventReminderService.sendDueReminders();

    assertThat(summary).isEqualTo(new EventReminderRunSummary(2, 0, 1, 0));
    verify(eventReminderBatchService).queueBatch(eq(bpRecipient), eq(DEADLINE_DATE), deadlinesCaptor.capture());
    assertThat(deadlinesCaptor.getValue()).containsExactly(termDeadline, expiryDeadline);
  }

  @Test
  void sendDueReminders_whenATermAndAnExpiryFallOnDifferentDates_thenTheyAreSeparateBatches() {
    var termDeadline = deadline(TermType.INITIAL.getDisplayName());
    var expiryDeadline = expiryDeadline(LATER_DEADLINE_DATE);
    mockRun(List.of(termDeadline), List.of(expiryDeadline), List.of(bpRecipient), List.of());

    var summary = eventReminderService.sendDueReminders();

    assertThat(summary).isEqualTo(new EventReminderRunSummary(2, 0, 2, 0));
    verify(eventReminderBatchService).queueBatch(bpRecipient, DEADLINE_DATE, List.of(termDeadline));
    verify(eventReminderBatchService).queueBatch(bpRecipient, LATER_DEADLINE_DATE, List.of(expiryDeadline));
  }

  @Test
  void sendDueReminders_whenOneOfTwoTypesWasAlreadyReminded_thenOnlyTheOtherIsCovered() {
    var termDeadline = deadline(TermType.INITIAL.getDisplayName());
    var expiryDeadline = expiryDeadline(DEADLINE_DATE);
    mockRun(
        List.of(termDeadline),
        List.of(expiryDeadline),
        List.of(bpRecipient),
        List.of(reminderRow(termDeadline, BP_ID)));

    eventReminderService.sendDueReminders();

    verify(eventReminderBatchService).queueBatch(bpRecipient, DEADLINE_DATE, List.of(expiryDeadline));
  }

  @Test
  void sendDueReminders_whenALicenceHasTwoLicensees_thenEachGetsItsOwnBatch() {
    var shellRecipient = new EventReminderRecipient(1, SHELL_ID, "Shell UK Ltd", "shell@example.com");
    mockRun(
        List.of(deadline(TermType.INITIAL.getDisplayName())),
        List.of(bpRecipient, shellRecipient),
        List.of());

    eventReminderService.sendDueReminders();

    verify(eventReminderBatchService, times(2)).queueBatch(any(), eq(DEADLINE_DATE), anyCollection());
    verify(eventReminderBatchService).queueBatch(eq(bpRecipient), any(), anyCollection());
    verify(eventReminderBatchService).queueBatch(eq(shellRecipient), any(), anyCollection());
  }

  @Test
  void sendDueReminders_whenTheOrganisationWasAlreadyReminded_thenItIsNotRemindedAgain() {
    var deadline = deadline(TermType.INITIAL.getDisplayName());
    mockRun(List.of(deadline), List.of(bpRecipient), List.of(reminderRow(deadline, BP_ID)));

    eventReminderService.sendDueReminders();

    verify(eventReminderBatchService, never()).queueBatch(any(), any(), anyCollection());
  }

  @Test
  void sendDueReminders_whenAnotherOrganisationWasReminded_thenThisOneStillIs() {
    var deadline = deadline(TermType.INITIAL.getDisplayName());
    mockRun(List.of(deadline), List.of(bpRecipient), List.of(reminderRow(deadline, SHELL_ID)));

    eventReminderService.sendDueReminders();

    verify(eventReminderBatchService).queueBatch(eq(bpRecipient), eq(DEADLINE_DATE), anyCollection());
  }

  @Test
  void sendDueReminders_whenOneBatchFails_thenTheOtherIsStillQueued() {
    var shellRecipient = new EventReminderRecipient(1, SHELL_ID, "Shell UK Ltd", "shell@example.com");
    mockRun(
        List.of(deadline(TermType.INITIAL.getDisplayName())),
        List.of(bpRecipient, shellRecipient),
        List.of());
    doThrow(new RuntimeException("notify unavailable"))
        .when(eventReminderBatchService).queueBatch(eq(bpRecipient), any(), anyCollection());

    var summary = eventReminderService.sendDueReminders();

    assertThat(summary).isEqualTo(new EventReminderRunSummary(1, 0, 1, 1));
    verify(eventReminderBatchService).queueBatch(eq(shellRecipient), any(), anyCollection());
  }

  @Test
  void sendDueReminders_whenTheLicenceHasNoRecipients_thenNoBatchIsQueued() {
    var deadline = deadline(TermType.INITIAL.getDisplayName());
    when(termOrPhaseEndDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of(deadline));
    when(workProgrammeActivityDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());
    when(licenceExpiryDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());
    when(eventReminderSuppressionService.getSuppressedLicenceIds(List.of(licence))).thenReturn(Set.of());
    when(eventReminderRecipientService.getRecipientsByLicenceId(List.of(licence))).thenReturn(Map.of());
    when(licenceReminderRepository.findAllByLicenceIn(List.of(licence))).thenReturn(List.of());

    eventReminderService.sendDueReminders();

    verify(eventReminderBatchService, never()).queueBatch(any(), any(), anyCollection());
  }

  @Test
  void sendDueReminders_whenASourceReturnsAnotherReminderType_thenItIsBatchedAndSuppressedUnderThatType() {
    var activityDeadline = new EventReminderDeadline(
        new WorkProgrammeActivity(),
        UUID.randomUUID(),
        licence,
        DEADLINE_DATE,
        "Drill well: Drill one exploration well",
        ReminderType.WORK_PROGRAMME_ACTIVITY);
    when(termOrPhaseEndDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());
    when(licenceExpiryDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());
    when(workProgrammeActivityDeadlineSource.getDeadlinesDueReminder())
        .thenReturn(List.of(activityDeadline));
    when(eventReminderSuppressionService.getSuppressedLicenceIds(List.of(licence)))
        .thenReturn(Set.of());
    when(eventReminderRecipientService.getRecipientsByLicenceId(List.of(licence)))
        .thenReturn(Map.of(licence.getId(), List.of(bpRecipient)));
    when(licenceReminderRepository.findAllByLicenceIn(List.of(licence)))
        .thenReturn(List.of());

    var summary = eventReminderService.sendDueReminders();

    assertThat(summary).isEqualTo(new EventReminderRunSummary(1, 0, 1, 0));
    verify(eventReminderBatchService).queueBatch(bpRecipient, DEADLINE_DATE, List.of(activityDeadline));
  }

  @Test
  void sendDueReminders_reportsLicenseesWithNoContactForTheUnsuppressedDeadlines() {
    var deadline = deadline("Initial Term");
    mockRun(List.of(deadline), List.of(bpRecipient), List.of());

    eventReminderService.sendDueReminders();

    verify(eventReminderMissingContactService).reportMissingContacts(List.of(deadline));
  }

  @Test
  void sendDueReminders_whenReportingMissingContactsFails_thenTheRunStillCompletes() {
    var deadline = deadline("Initial Term");
    mockRun(List.of(deadline), List.of(bpRecipient), List.of());
    when(eventReminderMissingContactService.reportMissingContacts(List.of(deadline)))
        .thenThrow(new IllegalStateException("Energy Portal unavailable"));

    var summary = eventReminderService.sendDueReminders();

    assertThat(summary.batchesQueued()).isEqualTo(1);
  }

  private void mockRun(
      List<EventReminderDeadline> deadlines,
      List<EventReminderRecipient> recipients,
      List<LicenceReminder> existingReminders
  ) {
    mockRun(deadlines, List.of(), recipients, existingReminders);
  }

  private void mockRun(
      List<EventReminderDeadline> termOrPhaseEndDeadlines,
      List<EventReminderDeadline> licenceExpiryDeadlines,
      List<EventReminderRecipient> recipients,
      List<LicenceReminder> existingReminders
  ) {
    when(termOrPhaseEndDeadlineSource.getDeadlinesDueReminder()).thenReturn(termOrPhaseEndDeadlines);
    when(workProgrammeActivityDeadlineSource.getDeadlinesDueReminder()).thenReturn(List.of());
    when(licenceExpiryDeadlineSource.getDeadlinesDueReminder()).thenReturn(licenceExpiryDeadlines);
    when(eventReminderSuppressionService.getSuppressedLicenceIds(List.of(licence))).thenReturn(Set.of());
    when(eventReminderRecipientService.getRecipientsByLicenceId(List.of(licence)))
        .thenReturn(Map.of(licence.getId(), recipients));
    when(licenceReminderRepository.findAllByLicenceIn(List.of(licence)))
        .thenReturn(existingReminders);
  }

  private LicenceReminder reminderRow(EventReminderDeadline deadline, Integer responsibleOrganisationId) {
    var reminder = new LicenceReminder();
    reminder.setOriginalEventId(deadline.originalEventId());
    reminder.setLicence(licence);
    reminder.setReminderType(deadline.reminderType());
    reminder.setResponsibleOrganisationId(responsibleOrganisationId);
    reminder.setDeadlineDate(deadline.deadlineDate());
    reminder.setNoticePeriod(NoticePeriod.SIX_MONTHS);
    return reminder;
  }

  private EventReminderDeadline deadline(String displayName) {
    return new EventReminderDeadline(
        new LicenceScheduleTerm(),
        UUID.randomUUID(),
        licence,
        DEADLINE_DATE,
        displayName,
        ReminderType.TERM_OR_PHASE_END);
  }

  private EventReminderDeadline expiryDeadline(LocalDate deadlineDate) {
    return new EventReminderDeadline(
        new LicenceScheduleExpiry(),
        null,
        licence,
        deadlineDate,
        "Licence expiry",
        ReminderType.LICENCE_EXPIRY);
  }
}
