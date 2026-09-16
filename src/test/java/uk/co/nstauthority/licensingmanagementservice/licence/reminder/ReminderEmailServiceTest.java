package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        BATCH_REFERENCE);

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
        BATCH_REFERENCE);

    verify(mergedTemplateBuilder).withMailMergeField(
        "DEADLINE_LIST",
        "* P001 — Initial Term\n* P001 — Phase B");

    verify(emailService).sendEmail(
        eq(mergedTemplate), any(EmailRecipient.class), any(DomainReference.class));
  }

  @Test
  void queueReminder_whenThereAreNoDeadlines_thenItFailsRatherThanSendingAnEmptyReminder() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
        reminderEmailService.queueReminder(RECIPIENT, DEADLINE_DATE, List.of(), BATCH_REFERENCE));
  }

  @Test
  void queueReminder_whenQueueingFails_thenItPropagatesSoTheBatchRollsBack() {
    mockTemplate();
    doThrow(new RuntimeException("notify unavailable"))
        .when(emailService)
        .sendEmail(eq(mergedTemplate), any(EmailRecipient.class), any(DomainReference.class));

    var deadlines = List.of(deadline(TermType.INITIAL.getDisplayName()));

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
        reminderEmailService.queueReminder(RECIPIENT, DEADLINE_DATE, deadlines, BATCH_REFERENCE));
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
        displayName);
  }
}
