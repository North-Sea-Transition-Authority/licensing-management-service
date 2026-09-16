package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

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
public class ReminderRecipientService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReminderRecipientService.class);

  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceContactService licenceContactService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public ReminderRecipientService(
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceContactService licenceContactService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceContactService = licenceContactService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public Map<Integer, List<ReminderRecipient>> getRecipientsByLicenceId(Collection<Licence> licences) {
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
        .collect(Collectors.groupingBy(ReminderRecipient::licenceId));
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

  private ReminderRecipient toRecipient(
      LicenceContact contact,
      Map<Integer, String> licenseeNamesByOrganisationId
  ) {
    var licensee = contact.getLicensee();
    var responsibleOrganisationId = licensee.getResponsibleOrganisationId();

    return new ReminderRecipient(
        licensee.getLicence().getId(),
        responsibleOrganisationId,
        licenseeNamesByOrganisationId.get(responsibleOrganisationId),
        contact.getContactEmail());
  }

  private void logLicenseesWithoutAContact(
      Collection<LicenceResponsibleOrganisation> licensees,
      Collection<LicenceContact> contacts
  ) {
    var licenseesWithAContact = contacts.stream()
        .map(LicenceContact::getLicensee)
        .collect(Collectors.toSet());

    licensees.stream()
        .filter(licensee -> !licenseesWithAContact.contains(licensee))
        .forEach(licensee -> LOGGER.warn(
            "No licence contact for organisation {} on licence {}, no reminder will be sent",
            licensee.getResponsibleOrganisationId(),
            licensee.getLicence().getId()));
  }
}
