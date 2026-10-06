package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
public class EventReminderMissingContactService {

  static final String DOMAIN_REFERENCE_TYPE = "LICENCE_REMINDER_MISSING_CONTACT";
  static final String GAP_LINE = "* %s — %s — %s due %s";
  static final String UNKNOWN_LICENSEE = "Unknown licensee";
  static final String CONTACTS_MANAGER_LOOKUP_PURPOSE =
      "Find the licence contacts managers to tell about licensees with no contact";

  private static final Logger LOGGER = LoggerFactory.getLogger(EventReminderMissingContactService.class);

  private final Clock clock;
  private final EventReminderRecipientService eventReminderRecipientService;
  private final LicenceReminderMissingContactRepository licenceReminderMissingContactRepository;
  private final TeamQueryService teamQueryService;
  private final EnergyPortalUserService energyPortalUserService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final EmailService emailService;

  public EventReminderMissingContactService(
      Clock clock,
      EventReminderRecipientService eventReminderRecipientService,
      LicenceReminderMissingContactRepository licenceReminderMissingContactRepository,
      TeamQueryService teamQueryService,
      EnergyPortalUserService energyPortalUserService,
      OrganisationUnitQueryService organisationUnitQueryService,
      EmailService emailService
  ) {
    this.clock = clock;
    this.eventReminderRecipientService = eventReminderRecipientService;
    this.licenceReminderMissingContactRepository = licenceReminderMissingContactRepository;
    this.teamQueryService = teamQueryService;
    this.energyPortalUserService = energyPortalUserService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.emailService = emailService;
  }

  @Transactional
  public int reportMissingContacts(List<EventReminderDeadline> deadlines) {
    if (deadlines.isEmpty()) {
      return 0;
    }

    var licences = deadlines.stream().map(EventReminderDeadline::licence).distinct().toList();
    var licenseesWithoutAContact = eventReminderRecipientService.getLicenseesWithoutAContact(licences);

    if (licenseesWithoutAContact.isEmpty()) {
      return 0;
    }

    var gaps = getUnreportedGaps(deadlines, licenseesWithoutAContact, licences);

    if (gaps.isEmpty()) {
      LOGGER.info("Every licensee without a contact has already been reported");
      return 0;
    }

    var contactsManagerEmails = getContactsManagerEmails();

    if (contactsManagerEmails.isEmpty()) {
      LOGGER.warn("{} licensees have no contact but there is no licence contacts manager to tell", gaps.size());
      return 0;
    }

    licenceReminderMissingContactRepository.saveAllAndFlush(toRecords(gaps));

    var reportReference = UUID.randomUUID();
    var mergedTemplate = emailService.getTemplate(GovukNotifyTemplate.REMINDER_MISSING_CONTACT_V1)
        .withMailMergeField("GAP_COUNT", String.valueOf(gaps.size()))
        .withMailMergeField("GAP_LIST", getGapList(gaps))
        .merge();

    contactsManagerEmails.forEach(email -> emailService.sendEmail(
        mergedTemplate,
        EmailRecipient.directEmailAddress(email),
        DomainReference.from(reportReference.toString(), DOMAIN_REFERENCE_TYPE)));

    LOGGER.info("Reported {} licensees with no contact to {} licence contacts managers",
        gaps.size(), contactsManagerEmails.size());

    return gaps.size();
  }

  private List<Gap> getUnreportedGaps(
      List<EventReminderDeadline> deadlines,
      List<LicenceResponsibleOrganisation> licenseesWithoutAContact,
      List<Licence> licences
  ) {
    var alreadyReported = licenceReminderMissingContactRepository.findAllByLicenceIn(licences)
        .stream()
        .map(reported -> new GapKey(
            reported.getOriginalEventId(),
            reported.getLicence().getId(),
            reported.getReminderType(),
            reported.getResponsibleOrganisationId(),
            reported.getDeadlineDate()))
        .collect(Collectors.toSet());

    var licenseesByLicenceId = licenseesWithoutAContact.stream()
        .collect(Collectors.groupingBy(licensee -> licensee.getLicence().getId()));

    return deadlines.stream()
        .flatMap(deadline -> licenseesByLicenceId.getOrDefault(deadline.licence().getId(), List.of()).stream()
            .map(licensee -> new Gap(deadline, licensee.getResponsibleOrganisationId())))
        .filter(gap -> !alreadyReported.contains(gap.key()))
        .toList();
  }

  private List<String> getContactsManagerEmails() {
    var wuaIds = teamQueryService.getAllTeamRolesWithRoles(Set.of(Role.LICENCE_CONTACTS_MANAGER))
        .stream()
        .filter(teamRole -> teamRole.getTeam().getTeamType() == TeamType.LICENCE_MANAGEMENT)
        .map(TeamRole::getWuaId)
        .distinct()
        .map(WebUserAccountId::from)
        .toList();

    if (wuaIds.isEmpty()) {
      return List.of();
    }

    return energyPortalUserService.findByWuaIds(wuaIds, CONTACTS_MANAGER_LOOKUP_PURPOSE)
        .stream()
        .map(EnergyPortalUserJson::emailAddress)
        .distinct()
        .toList();
  }

  private List<LicenceReminderMissingContact> toRecords(List<Gap> gaps) {
    var reportedAt = Instant.now(clock);

    return gaps.stream().map(gap -> toRecord(gap, reportedAt)).toList();
  }

  private LicenceReminderMissingContact toRecord(Gap gap, Instant reportedAt) {
    var missingContact = new LicenceReminderMissingContact();
    missingContact.setLicence(gap.deadline().licence());
    missingContact.setResponsibleOrganisationId(gap.responsibleOrganisationId());
    missingContact.setOriginalEventId(gap.deadline().originalEventId());
    missingContact.setReminderType(gap.deadline().reminderType());
    missingContact.setDeadlineDate(gap.deadline().deadlineDate());
    missingContact.setReportedAt(reportedAt);
    return missingContact;
  }

  private String getGapList(List<Gap> gaps) {
    var licenseeNamesByOrganisationId = getLicenseeNames(gaps);

    return gaps.stream()
        .sorted(Comparator.comparing((Gap gap) -> gap.deadline().licence().getLicenceReference())
            .thenComparing(gap -> gap.deadline().deadlineDate()))
        .map(gap -> GAP_LINE.formatted(
            gap.deadline().licence().getLicenceReference(),
            licenseeNamesByOrganisationId.getOrDefault(gap.responsibleOrganisationId(), UNKNOWN_LICENSEE),
            gap.deadline().reminderType().getDisplayName(),
            DateFormatUtil.convertToDisplayText(gap.deadline().deadlineDate())))
        .collect(Collectors.joining("\n"));
  }

  private Map<Integer, String> getLicenseeNames(List<Gap> gaps) {
    return organisationUnitQueryService.getOrganisationUnitNamesByIds(
        gaps.stream().map(Gap::responsibleOrganisationId).distinct().toList());
  }

  private record Gap(EventReminderDeadline deadline, Integer responsibleOrganisationId) {

    GapKey key() {
      return new GapKey(
          deadline.originalEventId(),
          deadline.licence().getId(),
          deadline.reminderType(),
          responsibleOrganisationId,
          deadline.deadlineDate());
    }
  }

  private record GapKey(
      UUID originalEventId,
      Integer licenceId,
      ReminderType reminderType,
      Integer responsibleOrganisationId,
      LocalDate deadlineDate
  ) {
  }
}
