package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContact;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContactService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@Service
public class EventReminderRecipientService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventReminderRecipientService.class);

  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceContactService licenceContactService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public EventReminderRecipientService(
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceContactService licenceContactService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceContactService = licenceContactService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public Map<Integer, List<EventReminderRecipient>> getRecipientsByLicenceId(Collection<Licence> licences) {
    if (licences.isEmpty()) {
      return Map.of();
    }

    var licensees = licenceResponsibleOrganisationService.getAllByLicenceIn(licences);

    if (licensees.isEmpty()) {
      return Map.of();
    }

    var contacts = licenceContactService.getContactsForLicensees(licensees);

    logLicenseesWithoutAContact(licensees, contacts);

    if (contacts.isEmpty()) {
      return Map.of();
    }

    var licenseeNamesByOrganisationId = organisationUnitQueryService.getOrganisationUnitNamesByIds(
        contacts.stream()
            .map(contact -> contact.getLicensee().getResponsibleOrganisationId())
            .distinct()
            .toList());

    return contacts.stream()
        .filter(contact -> hasLicenseeName(contact, licenseeNamesByOrganisationId))
        .map(contact -> toRecipient(contact, licenseeNamesByOrganisationId))
        .collect(Collectors.groupingBy(EventReminderRecipient::licenceId));
  }

  /**
   * The licensees on these licences that have no licence contact, and so cannot be sent a reminder.
   */
  public List<LicenceResponsibleOrganisation> getLicenseesWithoutAContact(Collection<Licence> licences) {
    if (licences.isEmpty()) {
      return List.of();
    }

    var licensees = licenceResponsibleOrganisationService.getAllByLicenceIn(licences);

    if (licensees.isEmpty()) {
      return List.of();
    }

    return getLicenseesWithoutAContact(licensees, licenceContactService.getContactsForLicensees(licensees));
  }

  private List<LicenceResponsibleOrganisation> getLicenseesWithoutAContact(
      Collection<LicenceResponsibleOrganisation> licensees,
      Collection<LicenceContact> contacts
  ) {
    var licenseesWithAContact = contacts.stream()
        .map(LicenceContact::getLicensee)
        .collect(Collectors.toSet());

    return licensees.stream()
        .filter(licensee -> !licenseesWithAContact.contains(licensee))
        .toList();
  }

  private boolean hasLicenseeName(LicenceContact contact, Map<Integer, String> licenseeNamesByOrganisationId) {
    var licensee = contact.getLicensee();

    if (licenseeNamesByOrganisationId.containsKey(licensee.getResponsibleOrganisationId())) {
      return true;
    }

    LOGGER.warn(
        "No organisation name found for organisation {} on licence {}, no reminder will be sent",
        licensee.getResponsibleOrganisationId(),
        licensee.getLicence().getId());
    return false;
  }

  private EventReminderRecipient toRecipient(
      LicenceContact contact,
      Map<Integer, String> licenseeNamesByOrganisationId
  ) {
    var licensee = contact.getLicensee();
    var responsibleOrganisationId = licensee.getResponsibleOrganisationId();

    return new EventReminderRecipient(
        licensee.getLicence().getId(),
        responsibleOrganisationId,
        licenseeNamesByOrganisationId.get(responsibleOrganisationId),
        contact.getContactEmail());
  }

  private void logLicenseesWithoutAContact(
      Collection<LicenceResponsibleOrganisation> licensees,
      Collection<LicenceContact> contacts
  ) {
    getLicenseesWithoutAContact(licensees, contacts).forEach(licensee -> LOGGER.warn(
            "No licence contact for organisation {} on licence {}, no reminder will be sent",
            licensee.getResponsibleOrganisationId(),
            licensee.getLicence().getId()));
  }
}
