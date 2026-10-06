package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
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
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class EventReminderMissingContactServiceTest {

  private static final Integer BP_ID = 181;
  private static final Integer SHELL_ID = 202;
  private static final Long CONTACTS_MANAGER_WUA_ID = 300048L;
  private static final String CONTACTS_MANAGER_EMAIL = "contacts.manager@nstauthority.co.uk";
  private static final LocalDate DEADLINE_DATE = LocalDate.of(2027, Month.MARCH, 31);
  private static final Instant REPORTED_AT = Instant.parse("2026-09-30T07:00:00Z");

  @Mock
  private Clock clock;

  @Mock
  private EventReminderRecipientService eventReminderRecipientService;

  @Mock
  private LicenceReminderMissingContactRepository licenceReminderMissingContactRepository;

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private EmailService emailService;

  @Mock
  private MergedTemplate.MergedTemplateBuilder mergedTemplateBuilder;

  @Mock
  private MergedTemplate mergedTemplate;

  @Captor
  private ArgumentCaptor<List<LicenceReminderMissingContact>> missingContactsCaptor;

  @Captor
  private ArgumentCaptor<EmailRecipient> emailRecipientCaptor;

  @Captor
  private ArgumentCaptor<DomainReference> domainReferenceCaptor;

  private EventReminderMissingContactService eventReminderMissingContactService;
  private Licence licence;

  @BeforeEach
  void setUp() {
    eventReminderMissingContactService = new EventReminderMissingContactService(
        clock,
        eventReminderRecipientService,
        licenceReminderMissingContactRepository,
        teamQueryService,
        energyPortalUserService,
        organisationUnitQueryService,
        emailService);

    licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();
  }

  @Test
  void reportMissingContacts_whenNoDeadlines_thenNothingHappens() {
    assertThat(eventReminderMissingContactService.reportMissingContacts(List.of())).isZero();

    verifyNoInteractions(eventReminderRecipientService, licenceReminderMissingContactRepository, emailService);
  }

  @Test
  void reportMissingContacts_whenEveryLicenseeHasAContact_thenNothingIsSentOrRecorded() {
    var deadline = deadline();
    when(eventReminderRecipientService.getLicenseesWithoutAContact(List.of(licence))).thenReturn(List.of());

    assertThat(eventReminderMissingContactService.reportMissingContacts(List.of(deadline))).isZero();

    verifyNoInteractions(licenceReminderMissingContactRepository, teamQueryService, emailService);
  }

  @Test
  void reportMissingContacts_whenALicenseeHasNoContact_thenTheContactsManagerIsToldAndTheGapIsRecorded() {
    var deadline = deadline();
    mockGap(deadline, licensee(BP_ID));
    mockContactsManagers(List.of(CONTACTS_MANAGER_EMAIL));
    mockEmail();
    when(clock.instant()).thenReturn(REPORTED_AT);
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(BP_ID)))
        .thenReturn(Map.of(BP_ID, "BP Exploration Alpha Ltd"));

    var reported = eventReminderMissingContactService.reportMissingContacts(List.of(deadline));

    assertThat(reported).isEqualTo(1);

    verify(licenceReminderMissingContactRepository).saveAllAndFlush(missingContactsCaptor.capture());
    assertThat(missingContactsCaptor.getValue())
        .extracting(
            LicenceReminderMissingContact::getLicence,
            LicenceReminderMissingContact::getResponsibleOrganisationId,
            LicenceReminderMissingContact::getOriginalEventId,
            LicenceReminderMissingContact::getReminderType,
            LicenceReminderMissingContact::getDeadlineDate,
            LicenceReminderMissingContact::getReportedAt)
        .containsExactly(tuple(
            licence, BP_ID, deadline.originalEventId(), ReminderType.TERM_OR_PHASE_END, DEADLINE_DATE, REPORTED_AT));

    verify(mergedTemplateBuilder).withMailMergeField("GAP_COUNT", "1");
    verify(mergedTemplateBuilder).withMailMergeField(
        "GAP_LIST", "* P001 — BP Exploration Alpha Ltd — Term or phase end due 31 March 2027");

    verify(emailService).sendEmail(eq(mergedTemplate), emailRecipientCaptor.capture(), domainReferenceCaptor.capture());
    assertThat(emailRecipientCaptor.getValue().getEmailAddress()).isEqualTo(CONTACTS_MANAGER_EMAIL);
    assertThat(domainReferenceCaptor.getValue().getDomainType())
        .isEqualTo(EventReminderMissingContactService.DOMAIN_REFERENCE_TYPE);
  }

  @Test
  void reportMissingContacts_whenThereAreTwoContactsManagers_thenEachGetsTheEmail() {
    var deadline = deadline();
    mockGap(deadline, licensee(BP_ID));
    mockContactsManagers(List.of(CONTACTS_MANAGER_EMAIL, "second.manager@nstauthority.co.uk"));
    mockEmail();
    when(clock.instant()).thenReturn(REPORTED_AT);
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(BP_ID)))
        .thenReturn(Map.of(BP_ID, "BP Exploration Alpha Ltd"));

    eventReminderMissingContactService.reportMissingContacts(List.of(deadline));

    verify(emailService, times(2)).sendEmail(eq(mergedTemplate), emailRecipientCaptor.capture(), domainReferenceCaptor.capture());
    assertThat(emailRecipientCaptor.getAllValues())
        .extracting(EmailRecipient::getEmailAddress)
        .containsExactly(CONTACTS_MANAGER_EMAIL, "second.manager@nstauthority.co.uk");
  }

  @Test
  void reportMissingContacts_whenTheGapWasAlreadyReported_thenItIsNotReportedAgain() {
    var deadline = deadline();
    var alreadyReported = new LicenceReminderMissingContact();
    alreadyReported.setLicence(licence);
    alreadyReported.setOriginalEventId(deadline.originalEventId());
    alreadyReported.setReminderType(ReminderType.TERM_OR_PHASE_END);
    alreadyReported.setResponsibleOrganisationId(BP_ID);
    alreadyReported.setDeadlineDate(DEADLINE_DATE);
    when(eventReminderRecipientService.getLicenseesWithoutAContact(List.of(licence))).thenReturn(List.of(licensee(BP_ID)));
    when(licenceReminderMissingContactRepository.findAllByLicenceIn(List.of(licence)))
        .thenReturn(List.of(alreadyReported));

    assertThat(eventReminderMissingContactService.reportMissingContacts(List.of(deadline))).isZero();

    verify(licenceReminderMissingContactRepository, never()).saveAllAndFlush(missingContactsCaptor.capture());
    verifyNoInteractions(teamQueryService, emailService);
  }

  @Test
  void reportMissingContacts_whenThereIsNoContactsManager_thenNothingIsSentOrRecorded() {
    var deadline = deadline();
    mockGap(deadline, licensee(BP_ID));
    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.LICENCE_CONTACTS_MANAGER))).thenReturn(List.of());

    assertThat(eventReminderMissingContactService.reportMissingContacts(List.of(deadline))).isZero();

    verify(licenceReminderMissingContactRepository, never()).saveAllAndFlush(missingContactsCaptor.capture());
    verifyNoInteractions(energyPortalUserService, emailService);
  }

  @Test
  void reportMissingContacts_whenTwoLicenseesOnOneLicenceHaveNoContact_thenBothAreListed() {
    var deadline = deadline();
    mockGap(deadline, licensee(BP_ID), licensee(SHELL_ID));
    mockContactsManagers(List.of(CONTACTS_MANAGER_EMAIL));
    mockEmail();
    when(clock.instant()).thenReturn(REPORTED_AT);
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(BP_ID, SHELL_ID)))
        .thenReturn(Map.of(BP_ID, "BP Exploration Alpha Ltd", SHELL_ID, "Shell UK Ltd"));

    var reported = eventReminderMissingContactService.reportMissingContacts(List.of(deadline));

    assertThat(reported).isEqualTo(2);
    verify(mergedTemplateBuilder).withMailMergeField("GAP_COUNT", "2");
    verify(mergedTemplateBuilder).withMailMergeField(
        "GAP_LIST",
        "* P001 — BP Exploration Alpha Ltd — Term or phase end due 31 March 2027\n"
            + "* P001 — Shell UK Ltd — Term or phase end due 31 March 2027");
  }

  @Test
  void reportMissingContacts_whenTheLicenseeNameIsUnknown_thenTheLineStillGoesOut() {
    var deadline = deadline();
    mockGap(deadline, licensee(BP_ID));
    mockContactsManagers(List.of(CONTACTS_MANAGER_EMAIL));
    mockEmail();
    when(clock.instant()).thenReturn(REPORTED_AT);
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(BP_ID))).thenReturn(Map.of());

    eventReminderMissingContactService.reportMissingContacts(List.of(deadline));

    verify(mergedTemplateBuilder).withMailMergeField(
        "GAP_LIST", "* P001 — Unknown licensee — Term or phase end due 31 March 2027");
  }

  @Test
  void reportMissingContacts_whenTheDeadlineIsALicenceExpiry_thenTheGapIsRecordedWithNoEventId() {
    var deadline = expiryDeadline();
    mockGap(deadline, licensee(BP_ID));
    mockContactsManagers(List.of(CONTACTS_MANAGER_EMAIL));
    mockEmail();
    when(clock.instant()).thenReturn(REPORTED_AT);
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(BP_ID)))
        .thenReturn(Map.of(BP_ID, "BP Exploration Alpha Ltd"));

    var reported = eventReminderMissingContactService.reportMissingContacts(List.of(deadline));

    assertThat(reported).isEqualTo(1);

    verify(licenceReminderMissingContactRepository).saveAllAndFlush(missingContactsCaptor.capture());
    assertThat(missingContactsCaptor.getValue())
        .extracting(
            LicenceReminderMissingContact::getOriginalEventId,
            LicenceReminderMissingContact::getReminderType,
            LicenceReminderMissingContact::getLicence)
        .containsExactly(tuple(null, ReminderType.LICENCE_EXPIRY, licence));

    verify(mergedTemplateBuilder).withMailMergeField(
        "GAP_LIST", "* P001 — BP Exploration Alpha Ltd — Licence expiry due 31 March 2027");
  }

  @Test
  void reportMissingContacts_whenAnExpiryGapWasAlreadyReported_thenItIsNotReportedAgain() {
    var deadline = expiryDeadline();
    var alreadyReported = new LicenceReminderMissingContact();
    alreadyReported.setLicence(licence);
    alreadyReported.setOriginalEventId(null);
    alreadyReported.setReminderType(ReminderType.LICENCE_EXPIRY);
    alreadyReported.setResponsibleOrganisationId(BP_ID);
    alreadyReported.setDeadlineDate(DEADLINE_DATE);
    when(eventReminderRecipientService.getLicenseesWithoutAContact(List.of(licence))).thenReturn(List.of(licensee(BP_ID)));
    when(licenceReminderMissingContactRepository.findAllByLicenceIn(List.of(licence)))
        .thenReturn(List.of(alreadyReported));

    assertThat(eventReminderMissingContactService.reportMissingContacts(List.of(deadline))).isZero();

    verifyNoInteractions(teamQueryService, emailService);
  }

  private void mockGap(EventReminderDeadline deadline, LicenceResponsibleOrganisation... licensees) {
    when(eventReminderRecipientService.getLicenseesWithoutAContact(List.of(deadline.licence())))
        .thenReturn(List.of(licensees));
    when(licenceReminderMissingContactRepository.findAllByLicenceIn(List.of(deadline.licence())))
        .thenReturn(List.of());
  }

  private void mockContactsManagers(List<String> emails) {
    var team = TeamTestUtil.newBuilder().withTeamType(TeamType.LICENCE_MANAGEMENT).build();
    var otherTeamRole = TeamRoleTestUtil.newBuilder()
        .withTeam(TeamTestUtil.newBuilder().withTeamType(TeamType.OFFSHORE_PRODUCTION_LICENSING).build())
        .withRole(Role.LICENCE_CONTACTS_MANAGER)
        .withWuaId(999L)
        .build();

    var teamRoles = new java.util.ArrayList<TeamRole>();
    teamRoles.add(otherTeamRole);
    var users = new java.util.ArrayList<EnergyPortalUserJson>();
    var wuaIds = new java.util.ArrayList<WebUserAccountId>();

    for (var index = 0; index < emails.size(); index++) {
      var wuaId = CONTACTS_MANAGER_WUA_ID + index;
      teamRoles.add(TeamRoleTestUtil.newBuilder()
          .withTeam(team)
          .withRole(Role.LICENCE_CONTACTS_MANAGER)
          .withWuaId(wuaId)
          .build());
      wuaIds.add(WebUserAccountId.from(wuaId));
      users.add(new EnergyPortalUserJson(
          wuaId, "Ms", "Contacts", "Manager", emails.get(index), "login", true, "0123456789", false));
    }

    when(teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.LICENCE_CONTACTS_MANAGER))).thenReturn(teamRoles);
    when(energyPortalUserService.findByWuaIds(
        wuaIds, EventReminderMissingContactService.CONTACTS_MANAGER_LOOKUP_PURPOSE))
        .thenReturn(users);
  }

  private void mockEmail() {
    when(emailService.getTemplate(GovukNotifyTemplate.REMINDER_MISSING_CONTACT_V1)).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplate);
  }

  private EventReminderDeadline deadline() {
    return new EventReminderDeadline(
        new LicenceScheduleTerm(),
        UUID.randomUUID(),
        licence,
        DEADLINE_DATE,
        "Initial Term",
        ReminderType.TERM_OR_PHASE_END);
  }

  private EventReminderDeadline expiryDeadline() {
    return new EventReminderDeadline(
        null,
        null,
        licence,
        DEADLINE_DATE,
        "Licence expiry",
        ReminderType.LICENCE_EXPIRY);
  }

  private LicenceResponsibleOrganisation licensee(Integer responsibleOrganisationId) {
    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(licence);
    licensee.setResponsibleOrganisationId(responsibleOrganisationId);
    return licensee;
  }
}
