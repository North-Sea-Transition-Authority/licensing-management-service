package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

  private static final LocalDate DEADLINE_DATE = LocalDate.of(2027, Month.DECEMBER, 31);
  private static final Integer BP_ID = 181;
  private static final Integer SHELL_ID = 202;

  @Mock
  private ReminderDeadlineSource termOrPhaseEndDeadlineSource;

  @Mock
  private ReminderSuppressionService reminderSuppressionService;

  @Mock
  private ReminderRecipientService reminderRecipientService;

  @Mock
  private ReminderBatchService reminderBatchService;

  @Mock
  private LicenceReminderRepository licenceReminderRepository;

  @Captor
  private ArgumentCaptor<List<ReminderDeadline>> deadlinesCaptor;

  private ReminderService reminderService;
  private Licence licence;
  private ReminderRecipient bpRecipient;

  @BeforeEach
  void setUp() {
    reminderService = new ReminderService(
        List.of(termOrPhaseEndDeadlineSource),
        reminderSuppressionService,
        reminderRecipientService,
        reminderBatchService,
        licenceReminderRepository);
    licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();
    bpRecipient = new ReminderRecipient(1, BP_ID, "BP Exploration Alpha Ltd", "bp@example.com");
  }

  @Test
  void sendDueReminders_whenNothingIsDue_thenNoBatchIsQueued() {
    when(termOrPhaseEndDeadlineSource.getDeadlinesDueReminder(NoticePeriod.SIX_MONTHS)).thenReturn(List.of());

    var summary = reminderService.sendDueReminders(NoticePeriod.SIX_MONTHS);

    assertThat(summary).isEqualTo(new ReminderRunSummary(0, 0, 0, 0));
    verify(reminderBatchService, never()).queueBatch(any(), any(), anyCollection(), any());
    verify(reminderSuppressionService, never()).getSuppressedLicenceIds(anyCollection());
  }

  @Test
  void sendDueReminders_whenTheLicenceIsSuppressed_thenNoBatchIsQueued() {
    var deadline = deadline(TermType.INITIAL.getDisplayName());
    when(termOrPhaseEndDeadlineSource.getDeadlinesDueReminder(NoticePeriod.SIX_MONTHS)).thenReturn(List.of(deadline));
    when(reminderSuppressionService.getSuppressedLicenceIds(List.of(licence))).thenReturn(Set.of(1));

    var summary = reminderService.sendDueReminders(NoticePeriod.SIX_MONTHS);

    assertThat(summary).isEqualTo(new ReminderRunSummary(1, 1, 0, 0));
    verify(reminderBatchService, never()).queueBatch(any(), any(), anyCollection(), any());
    verify(reminderRecipientService, never()).getRecipientsByLicenceId(anyCollection());
  }

  @Test
  void sendDueReminders_whenTwoDeadlinesShareARecipientAndDate_thenOneBatchCoversBoth() {
    var initialTerm = deadline(TermType.INITIAL.getDisplayName());
    var secondTerm = deadline(TermType.SECOND.getDisplayName());
    mockRun(List.of(initialTerm, secondTerm), List.of(bpRecipient), List.of());

    reminderService.sendDueReminders(NoticePeriod.SIX_MONTHS);

    verify(reminderBatchService).queueBatch(
        eq(bpRecipient), eq(DEADLINE_DATE), deadlinesCaptor.capture(), eq(NoticePeriod.SIX_MONTHS));

    assertThat(deadlinesCaptor.getValue()).containsExactly(initialTerm, secondTerm);
  }

  @Test
  void sendDueReminders_whenALicenceHasTwoLicensees_thenEachGetsItsOwnBatch() {
    var shellRecipient = new ReminderRecipient(1, SHELL_ID, "Shell UK Ltd", "shell@example.com");
    mockRun(
        List.of(deadline(TermType.INITIAL.getDisplayName())),
        List.of(bpRecipient, shellRecipient),
        List.of());

    reminderService.sendDueReminders(NoticePeriod.SIX_MONTHS);

    verify(reminderBatchService, times(2)).queueBatch(
        any(), eq(DEADLINE_DATE), anyCollection(), eq(NoticePeriod.SIX_MONTHS));
    verify(reminderBatchService).queueBatch(eq(bpRecipient), any(), anyCollection(), any());
    verify(reminderBatchService).queueBatch(eq(shellRecipient), any(), anyCollection(), any());
  }

  @Test
  void sendDueReminders_whenTheOrganisationWasAlreadyReminded_thenItIsNotRemindedAgain() {
    var deadline = deadline(TermType.INITIAL.getDisplayName());
    mockRun(List.of(deadline), List.of(bpRecipient), List.of(reminderRow(deadline, BP_ID)));

    reminderService.sendDueReminders(NoticePeriod.SIX_MONTHS);

    verify(reminderBatchService, never()).queueBatch(any(), any(), anyCollection(), any());
  }

  @Test
  void sendDueReminders_whenAnotherOrganisationWasReminded_thenThisOneStillIs() {
    var deadline = deadline(TermType.INITIAL.getDisplayName());
    mockRun(List.of(deadline), List.of(bpRecipient), List.of(reminderRow(deadline, SHELL_ID)));

    reminderService.sendDueReminders(NoticePeriod.SIX_MONTHS);

    verify(reminderBatchService).queueBatch(
        eq(bpRecipient), eq(DEADLINE_DATE), anyCollection(), eq(NoticePeriod.SIX_MONTHS));
  }

  @Test
  void sendDueReminders_whenOneBatchFails_thenTheOtherIsStillQueued() {
    var shellRecipient = new ReminderRecipient(1, SHELL_ID, "Shell UK Ltd", "shell@example.com");
    mockRun(
        List.of(deadline(TermType.INITIAL.getDisplayName())),
        List.of(bpRecipient, shellRecipient),
        List.of());
    doThrow(new RuntimeException("notify unavailable"))
        .when(reminderBatchService).queueBatch(eq(bpRecipient), any(), anyCollection(), any());

    var summary = reminderService.sendDueReminders(NoticePeriod.SIX_MONTHS);

    assertThat(summary).isEqualTo(new ReminderRunSummary(1, 0, 1, 1));
    verify(reminderBatchService).queueBatch(eq(shellRecipient), any(), anyCollection(), any());
  }

  @Test
  void sendDueReminders_whenTheLicenceHasNoRecipients_thenNoBatchIsQueued() {
    var deadline = deadline(TermType.INITIAL.getDisplayName());
    when(termOrPhaseEndDeadlineSource.getDeadlinesDueReminder(NoticePeriod.SIX_MONTHS)).thenReturn(List.of(deadline));
    when(reminderSuppressionService.getSuppressedLicenceIds(List.of(licence))).thenReturn(Set.of());
    when(reminderRecipientService.getRecipientsByLicenceId(List.of(licence))).thenReturn(Map.of());
    when(licenceReminderRepository.findAllByOriginalEventIdInAndNoticePeriod(
        List.of(deadline.originalEventId()), NoticePeriod.SIX_MONTHS)).thenReturn(List.of());

    reminderService.sendDueReminders(NoticePeriod.SIX_MONTHS);

    verify(reminderBatchService, never()).queueBatch(any(), any(), anyCollection(), any());
  }

  private void mockRun(
      List<ReminderDeadline> deadlines,
      List<ReminderRecipient> recipients,
      List<LicenceReminder> existingReminders
  ) {
    when(termOrPhaseEndDeadlineSource.getDeadlinesDueReminder(NoticePeriod.SIX_MONTHS)).thenReturn(deadlines);
    when(reminderSuppressionService.getSuppressedLicenceIds(List.of(licence))).thenReturn(Set.of());
    when(reminderRecipientService.getRecipientsByLicenceId(List.of(licence)))
        .thenReturn(Map.of(licence.getId(), recipients));
    when(licenceReminderRepository.findAllByOriginalEventIdInAndNoticePeriod(
        deadlines.stream().map(ReminderDeadline::originalEventId).distinct().toList(),
        NoticePeriod.SIX_MONTHS))
        .thenReturn(existingReminders);
  }

  private LicenceReminder reminderRow(ReminderDeadline deadline, Integer responsibleOrganisationId) {
    var reminder = new LicenceReminder();
    reminder.setOriginalEventId(deadline.originalEventId());
    reminder.setResponsibleOrganisationId(responsibleOrganisationId);
    reminder.setDeadlineDate(deadline.deadlineDate());
    reminder.setNoticePeriod(NoticePeriod.SIX_MONTHS);
    return reminder;
  }

  private ReminderDeadline deadline(String displayName) {
    return new ReminderDeadline(
        new LicenceScheduleTerm(),
        UUID.randomUUID(),
        licence,
        DEADLINE_DATE,
        displayName);
  }
}
