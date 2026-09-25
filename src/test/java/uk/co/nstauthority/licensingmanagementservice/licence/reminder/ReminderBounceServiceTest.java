package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;

@ExtendWith(MockitoExtension.class)
class ReminderBounceServiceTest {

  private static final Instant REPORTED_AT = Instant.parse("2026-09-17T08:00:00Z");
  private static final LocalDate DEADLINE_DATE = LocalDate.of(2027, Month.MARCH, 31);
  private static final Integer BP_ID = 181;
  private static final String APPROVALS_EMAIL = "approvals@nstauthority.co.uk";

  @Mock
  private Clock clock;

  @Mock
  private LicenceReminderRepository licenceReminderRepository;

  @Mock
  private LicenceReminderBounceRepository licenceReminderBounceRepository;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private EmailService emailService;

  @Mock
  private MergedTemplate.MergedTemplateBuilder mergedTemplateBuilder;

  @Mock
  private MergedTemplate mergedTemplate;

  @Captor
  private ArgumentCaptor<List<LicenceReminderBounce>> bouncesCaptor;

  @Captor
  private ArgumentCaptor<EmailRecipient> emailRecipientCaptor;

  @Captor
  private ArgumentCaptor<DomainReference> domainReferenceCaptor;

  private ReminderBounceService reminderBounceService;

  @BeforeEach
  void setUp() {
    var customerConfigurationProperties = new CustomerConfigurationProperties(
        "North Sea Transition Authority", "NSTA", "https://example.com/privacy", APPROVALS_EMAIL);

    reminderBounceService = new ReminderBounceService(
        clock,
        licenceReminderRepository,
        licenceReminderBounceRepository,
        organisationUnitQueryService,
        customerConfigurationProperties,
        emailService);
  }

  @Test
  void reportBounces_whenNothingHasBounced_thenNothingIsSentOrRecorded() {
    when(licenceReminderRepository.findAllBouncedAndUnreported()).thenReturn(List.of());

    assertThat(reminderBounceService.reportBounces()).isZero();

    verifyNoInteractions(licenceReminderBounceRepository, organisationUnitQueryService, emailService);
  }

  @Test
  void reportBounces_whenAReminderHasBounced_thenTheApprovalsInboxIsToldAndTheBounceIsRecorded() {
    var batchReference = UUID.randomUUID();
    var bounced = bouncedReminder(batchReference, "P001", ReminderType.TERM_OR_PHASE_END, DEADLINE_DATE);
    mockRun(List.of(bounced), Map.of(BP_ID, "BP Exploration Alpha Ltd"));

    var reported = reminderBounceService.reportBounces();

    assertThat(reported).isEqualTo(1);

    verify(licenceReminderBounceRepository).saveAllAndFlush(bouncesCaptor.capture());
    assertThat(bouncesCaptor.getValue())
        .extracting(LicenceReminderBounce::getNotificationBatchReference, LicenceReminderBounce::getReportedAt)
        .containsExactly(tuple(batchReference, REPORTED_AT));

    verify(mergedTemplateBuilder).withMailMergeField("BOUNCE_COUNT", "1");
    verify(mergedTemplateBuilder).withMailMergeField(
        "BOUNCE_LIST",
        "* P001 — BP Exploration Alpha Ltd — bp@example.com — Term or phase end due 31 March 2027"
            + " — Email address does not exist");

    verify(emailService).sendEmail(eq(mergedTemplate), emailRecipientCaptor.capture(), domainReferenceCaptor.capture());
    assertThat(emailRecipientCaptor.getValue().getEmailAddress()).isEqualTo(APPROVALS_EMAIL);
    assertThat(domainReferenceCaptor.getValue().getDomainType()).isEqualTo(ReminderBounceService.DOMAIN_REFERENCE_TYPE);
  }

  @Test
  void reportBounces_whenOneBouncedEmailCoveredTwoDeadlines_thenBothAreListedButTheBounceIsRecordedOnce() {
    var batchReference = UUID.randomUUID();
    var secondTerm = bouncedReminder(batchReference, "P001", ReminderType.TERM_OR_PHASE_END, DEADLINE_DATE);
    var expiry = bouncedReminder(batchReference, "P001", ReminderType.LICENCE_EXPIRY, DEADLINE_DATE.minusDays(1));
    mockRun(List.of(secondTerm, expiry), Map.of(BP_ID, "BP Exploration Alpha Ltd"));

    var reported = reminderBounceService.reportBounces();

    assertThat(reported).isEqualTo(2);

    verify(licenceReminderBounceRepository).saveAllAndFlush(bouncesCaptor.capture());
    assertThat(bouncesCaptor.getValue())
        .extracting(LicenceReminderBounce::getNotificationBatchReference)
        .containsExactly(batchReference);

    verify(mergedTemplateBuilder).withMailMergeField("BOUNCE_COUNT", "2");
    verify(mergedTemplateBuilder).withMailMergeField(
        "BOUNCE_LIST",
        "* P001 — BP Exploration Alpha Ltd — bp@example.com — Licence expiry due 30 March 2027 — Email address"
            + " does not exist\n"
            + "* P001 — BP Exploration Alpha Ltd — bp@example.com — Term or phase end due 31 March 2027 — Email"
            + " address does not exist");
  }

  @Test
  void reportBounces_whenTheLicenseeNameIsUnknown_thenTheLineStillGoesOut() {
    var bounced = bouncedReminder(UUID.randomUUID(), "P001", ReminderType.WORK_PROGRAMME_ACTIVITY, DEADLINE_DATE);
    mockRun(List.of(bounced), Map.of());

    reminderBounceService.reportBounces();

    verify(mergedTemplateBuilder).withMailMergeField(
        "BOUNCE_LIST",
        "* P001 — Unknown licensee — bp@example.com — Work programme activity due 31 March 2027 — Email address"
            + " does not exist");
  }

  private void mockRun(List<BouncedReminder> bouncedReminders, Map<Integer, String> licenseeNames) {
    when(clock.instant()).thenReturn(REPORTED_AT);
    when(licenceReminderRepository.findAllBouncedAndUnreported()).thenReturn(bouncedReminders);
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(BP_ID))).thenReturn(licenseeNames);
    when(emailService.getTemplate(GovukNotifyTemplate.REMINDER_BOUNCED_V1)).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplate);
  }

  private BouncedReminder bouncedReminder(
      UUID batchReference,
      String licenceReference,
      ReminderType reminderType,
      LocalDate deadlineDate
  ) {
    return new BouncedReminderStub(
        batchReference,
        licenceReference,
        BP_ID,
        reminderType,
        deadlineDate,
        "bp@example.com",
        "Email address does not exist");
  }

  private record BouncedReminderStub(
      UUID notificationBatchReference,
      String licenceReference,
      Integer responsibleOrganisationId,
      ReminderType reminderType,
      LocalDate deadlineDate,
      String recipient,
      String failureReason
  ) implements BouncedReminder {

    @Override
    public UUID getNotificationBatchReference() {
      return notificationBatchReference;
    }

    @Override
    public String getLicenceReference() {
      return licenceReference;
    }

    @Override
    public Integer getResponsibleOrganisationId() {
      return responsibleOrganisationId;
    }

    @Override
    public ReminderType getReminderType() {
      return reminderType;
    }

    @Override
    public LocalDate getDeadlineDate() {
      return deadlineDate;
    }

    @Override
    public String getRecipient() {
      return recipient;
    }

    @Override
    public String getFailureReason() {
      return failureReason;
    }
  }
}
