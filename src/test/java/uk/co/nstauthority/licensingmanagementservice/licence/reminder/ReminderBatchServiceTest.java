package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@ExtendWith(MockitoExtension.class)
class ReminderBatchServiceTest {

  private static final Instant QUEUED_AT = Instant.parse("2027-06-30T07:00:00Z");
  private static final LocalDate DEADLINE_DATE = LocalDate.of(2027, Month.DECEMBER, 31);
  private static final ReminderRecipient RECIPIENT = new ReminderRecipient(
      1, 181, "BP Exploration Alpha Ltd", "bp@example.com");

  @Mock
  private Clock clock;

  @Mock
  private LicenceReminderRepository licenceReminderRepository;

  @Mock
  private ReminderEmailService reminderEmailService;

  @InjectMocks
  private ReminderBatchService reminderBatchService;

  @Captor
  private ArgumentCaptor<List<LicenceReminder>> remindersCaptor;

  @Test
  void queueBatch_writesOneRowPerDeadlineSharingOneBatchReference() {
    mockClock();
    var initialTerm = deadline(TermType.INITIAL.getDisplayName());
    var secondTerm = deadline(TermType.SECOND.getDisplayName());

    reminderBatchService.queueBatch(
        RECIPIENT, DEADLINE_DATE, List.of(initialTerm, secondTerm), NoticePeriod.SIX_MONTHS);

    verify(licenceReminderRepository).saveAllAndFlush(remindersCaptor.capture());

    var reminders = remindersCaptor.getValue();
    assertThat(reminders).hasSize(2);
    assertThat(reminders)
        .extracting(LicenceReminder::getNotificationBatchReference)
        .containsOnly(reminders.getFirst().getNotificationBatchReference());
    assertThat(reminders)
        .extracting(
            LicenceReminder::getOriginalEventId,
            LicenceReminder::getResponsibleOrganisationId,
            LicenceReminder::getDeadlineDate,
            LicenceReminder::getNoticePeriod,
            LicenceReminder::getQueuedAt)
        .containsExactly(
            Tuple.tuple(
                initialTerm.originalEventId(), 181, DEADLINE_DATE, NoticePeriod.SIX_MONTHS, QUEUED_AT),
            Tuple.tuple(
                secondTerm.originalEventId(), 181, DEADLINE_DATE, NoticePeriod.SIX_MONTHS, QUEUED_AT));
  }

  @Test
  void queueBatch_flushesTheRowsBeforeQueueingTheEmail() {
    mockClock();
    var deadline = deadline(TermType.INITIAL.getDisplayName());

    reminderBatchService.queueBatch(
        RECIPIENT, DEADLINE_DATE, List.of(deadline), NoticePeriod.SIX_MONTHS);

    InOrder inOrder = Mockito.inOrder(licenceReminderRepository, reminderEmailService);
    inOrder.verify(licenceReminderRepository).saveAllAndFlush(anyCollection());
    inOrder.verify(reminderEmailService).queueReminder(
        eq(RECIPIENT), eq(DEADLINE_DATE), anyCollection(), any(UUID.class));
  }

  @Test
  void queueBatch_whenTheRowsAreRejected_thenNoEmailIsQueued() {
    mockClock();
    doThrow(new RuntimeException("duplicate reminder"))
        .when(licenceReminderRepository).saveAllAndFlush(anyCollection());

    var deadlines = List.of(deadline(TermType.INITIAL.getDisplayName()));

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
        reminderBatchService.queueBatch(RECIPIENT, DEADLINE_DATE, deadlines, NoticePeriod.SIX_MONTHS));

    verify(reminderEmailService, never()).queueReminder(any(), any(), anyCollection(), any());
  }

  @Test
  void queueBatch_passesTheSameBatchReferenceToTheEmailAsTheRows() {
    mockClock();
    var batchReferenceCaptor = ArgumentCaptor.forClass(UUID.class);

    reminderBatchService.queueBatch(
        RECIPIENT,
        DEADLINE_DATE,
        List.of(deadline(TermType.INITIAL.getDisplayName())),
        NoticePeriod.SIX_MONTHS);

    verify(licenceReminderRepository).saveAllAndFlush(remindersCaptor.capture());
    verify(reminderEmailService).queueReminder(
        eq(RECIPIENT), eq(DEADLINE_DATE), anyCollection(), batchReferenceCaptor.capture());

    assertThat(batchReferenceCaptor.getValue())
        .isEqualTo(remindersCaptor.getValue().getFirst().getNotificationBatchReference());
  }

  private void mockClock() {
    when(clock.instant()).thenReturn(QUEUED_AT);
  }

  private ReminderDeadline deadline(String displayName) {
    Licence licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();

    return new ReminderDeadline(
        new LicenceScheduleTerm(),
        UUID.randomUUID(),
        licence,
        DEADLINE_DATE,
        displayName);
  }
}
