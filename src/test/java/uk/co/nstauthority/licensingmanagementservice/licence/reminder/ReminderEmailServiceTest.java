package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;

@ExtendWith(MockitoExtension.class)
class ReminderEmailServiceTest {

  private static final LocalDate DEADLINE_DATE = LocalDate.of(2027, Month.DECEMBER, 31);
  private static final UUID BATCH_REFERENCE = UUID.randomUUID();
  private static final ReminderRecipient RECIPIENT = new ReminderRecipient(
      1, 181, "BP Exploration Alpha Ltd", "bp@example.com");

  @Mock
  private EmailService emailService;

  @Mock
  private MergedTemplate.MergedTemplateBuilder mergedTemplateBuilder;

  @Mock
  private MergedTemplate mergedTemplate;

  @InjectMocks
  private ReminderEmailService reminderEmailService;

  @Captor
  private ArgumentCaptor<EmailRecipient> emailRecipientCaptor;

  @Captor
  private ArgumentCaptor<DomainReference> domainReferenceCaptor;

  @Test
  void queueReminder_mergesTheRecipientAndDeadlineAndSendsToTheContact() {
    mockTemplate();

    reminderEmailService.queueReminder(
        RECIPIENT,
        DEADLINE_DATE,
        List.of(deadline(TermType.INITIAL.getDisplayName())),
        BATCH_REFERENCE,
        ReminderType.TERM_OR_PHASE_END);

    verify(mergedTemplateBuilder).withMailMergeField("LICENCE_REFERENCE", "P001");
    verify(mergedTemplateBuilder).withMailMergeField("LICENSEE_NAME", "BP Exploration Alpha Ltd");
    verify(mergedTemplateBuilder).withMailMergeField("DEADLINE_DATE", "31 December 2027");
    verify(mergedTemplateBuilder).withMailMergeField("DEADLINE_LIST", "* P001 — Initial Term");

    verify(emailService).sendEmail(
        eq(mergedTemplate),
        emailRecipientCaptor.capture(),
        domainReferenceCaptor.capture());

    assertThat(emailRecipientCaptor.getValue().getEmailAddress()).isEqualTo("bp@example.com");
    assertThat(domainReferenceCaptor.getValue().getDomainId()).isEqualTo(BATCH_REFERENCE.toString());
    assertThat(domainReferenceCaptor.getValue().getDomainType())
        .isEqualTo(ReminderEmailService.DOMAIN_REFERENCE_TYPE);
  }

  @Test
  void queueReminder_whenSeveralDeadlinesShareTheDate_thenTheyAreListedInOneEmail() {
    mockTemplate();

    reminderEmailService.queueReminder(
        RECIPIENT,
        DEADLINE_DATE,
        List.of(
            deadline(PhaseType.PHASE_B.getDisplayName()),
            deadline(TermType.INITIAL.getDisplayName())),
        BATCH_REFERENCE,
        ReminderType.TERM_OR_PHASE_END);

    verify(mergedTemplateBuilder).withMailMergeField(
        "DEADLINE_LIST",
        "* P001 — Initial Term\n* P001 — Phase B");

    verify(emailService).sendEmail(
        eq(mergedTemplate), any(EmailRecipient.class), any(DomainReference.class));
  }

  @Test
  void queueReminder_whenTheReminderIsForExpiry_thenTheExpiryTemplateIsUsed() {
    when(emailService.getTemplate(GovukNotifyTemplate.LICENCE_EXPIRY_REMINDER_V1))
        .thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplate);

    reminderEmailService.queueReminder(
        RECIPIENT,
        DEADLINE_DATE,
        List.of(deadline("Licence expiry")),
        BATCH_REFERENCE,
        ReminderType.LICENCE_EXPIRY);

    verify(mergedTemplateBuilder).withMailMergeField("DEADLINE_LIST", "* P001 — Licence expiry");
    verify(emailService).sendEmail(
        eq(mergedTemplate), any(EmailRecipient.class), any(DomainReference.class));
  }

  @Test
  void queueReminder_whenThereAreNoDeadlines_thenItFailsRatherThanSendingAnEmptyReminder() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
        reminderEmailService.queueReminder(RECIPIENT, DEADLINE_DATE, List.of(), BATCH_REFERENCE,
        ReminderType.TERM_OR_PHASE_END));
  }

  @Test
  void queueReminder_whenTheReminderIsForAnotherScheduleEvent_thenTheOtherScheduleEventTemplateIsUsed() {
    when(emailService.getTemplate(GovukNotifyTemplate.OTHER_SCHEDULE_EVENT_REMINDER_V1))
        .thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplate);
    var eventDeadline = new ReminderDeadline(
        new OtherScheduleEvent(),
        UUID.randomUUID(),
        LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build(),
        DEADLINE_DATE,
        "Mandatory relinquishment: Relinquish 50% of the licensed area",
        ReminderType.OTHER_SCHEDULE_EVENT);

    reminderEmailService.queueReminder(
        RECIPIENT,
        DEADLINE_DATE,
        List.of(eventDeadline),
        BATCH_REFERENCE,
        ReminderType.OTHER_SCHEDULE_EVENT);

    verify(mergedTemplateBuilder).withMailMergeField(
        "DEADLINE_LIST",
        "* P001 — Mandatory relinquishment: Relinquish 50% of the licensed area");
    verify(emailService).sendEmail(
        eq(mergedTemplate),
        refEq(EmailRecipient.directEmailAddress(RECIPIENT.contactEmail())),
        refEq(DomainReference.from(BATCH_REFERENCE.toString(), ReminderEmailService.DOMAIN_REFERENCE_TYPE)));
  }

  @Test
  void queueReminder_whenTheReminderIsForAnActivity_thenTheActivityTemplateIsUsed() {
    when(emailService.getTemplate(GovukNotifyTemplate.WORK_PROGRAMME_ACTIVITY_REMINDER_V1))
        .thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplate);
    var activityDeadline = new ReminderDeadline(
        new WorkProgrammeActivity(),
        UUID.randomUUID(),
        LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build(),
        DEADLINE_DATE,
        "Drill well: Drill one exploration well",
        ReminderType.WORK_PROGRAMME_ACTIVITY);

    reminderEmailService.queueReminder(
        RECIPIENT,
        DEADLINE_DATE,
        List.of(activityDeadline),
        BATCH_REFERENCE,
        ReminderType.WORK_PROGRAMME_ACTIVITY);

    verify(mergedTemplateBuilder).withMailMergeField("DEADLINE_LIST", "* P001 — Drill well: Drill one exploration well");
    verify(emailService).sendEmail(
        eq(mergedTemplate),
        refEq(EmailRecipient.directEmailAddress(RECIPIENT.contactEmail())),
        refEq(DomainReference.from(BATCH_REFERENCE.toString(), ReminderEmailService.DOMAIN_REFERENCE_TYPE)));
  }

  @Test
  void queueReminder_whenQueueingFails_thenItPropagatesSoTheBatchRollsBack() {
    mockTemplate();
    doThrow(new RuntimeException("notify unavailable"))
        .when(emailService)
        .sendEmail(eq(mergedTemplate), any(EmailRecipient.class), any(DomainReference.class));

    var deadlines = List.of(deadline(TermType.INITIAL.getDisplayName()));

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
        reminderEmailService.queueReminder(RECIPIENT, DEADLINE_DATE, deadlines, BATCH_REFERENCE,
        ReminderType.TERM_OR_PHASE_END));
  }

  private void mockTemplate() {
    when(emailService.getTemplate(GovukNotifyTemplate.TERM_OR_PHASE_END_REMINDER_V1))
        .thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplate);
  }

  private ReminderDeadline deadline(String displayName) {
    var licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();

    return new ReminderDeadline(
        new LicenceScheduleTerm(),
        UUID.randomUUID(),
        licence,
        DEADLINE_DATE,
        displayName,
        ReminderType.TERM_OR_PHASE_END);
  }
}
