package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsLicenceContacts;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsLicenceContacts.JoiningLicenseeContact;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsLicenceContactsEpmqMessage;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.PearsLicenceRefreshService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationRepository;

/**
 * Applies the licence contacts PEARS publishes to EPMQ onto the LMS {@link LicenceContact} records.
 *
 * <p>PEARS is the source of truth for who the contact is on a licence a licensee has just joined, so an incoming
 * email overwrites whatever LMS holds for that licensee. Nothing is removed on the strength of a message: a contact
 * without an email address is left alone rather than cleared.</p>
 */
@Service
public class PearsLicenceContactsUpdateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsLicenceContactsUpdateService.class);

  private final PearsLicenceRefreshService pearsLicenceRefreshService;
  private final LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;
  private final LicenceContactRepository licenceContactRepository;

  PearsLicenceContactsUpdateService(
      PearsLicenceRefreshService pearsLicenceRefreshService,
      LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository,
      LicenceContactRepository licenceContactRepository
  ) {
    this.pearsLicenceRefreshService = pearsLicenceRefreshService;
    this.licenceResponsibleOrganisationRepository = licenceResponsibleOrganisationRepository;
    this.licenceContactRepository = licenceContactRepository;
  }

  /**
   * Updates the LMS licence contacts named by one PEARS message.
   *
   * <p>The licence and its responsible organisations are refreshed from the Energy Portal first, so the contacts are
   * applied to the licensees the licence actually holds now rather than to whatever the last hourly refresh left
   * behind. A message about a licence the Energy Portal does not hold is skipped entirely.</p>
   *
   * <p>Anything the message names that LMS cannot resolve is logged and skipped rather than thrown, so a message
   * LMS can never act on does not sit on the queue being redelivered forever. A failure to reach the Energy Portal
   * is deliberately left to propagate, so the message is redelivered and tried again.</p>
   */
  @Transactional
  public void updateContacts(PearsLicenceContactsEpmqMessage message) {
    var correlationId = message.getCorrelationId();
    var licenceId = parseId(message.getLicenceId());

    if (licenceId == null) {
      LOGGER.error("Ignoring PEARS licence contacts with unusable licence id {}, correlationId: {}",
          message.getLicenceId(), correlationId);
      return;
    }

    var licence = pearsLicenceRefreshService.refreshLicence(licenceId);

    if (licence.isEmpty()) {
      LOGGER.warn("Ignoring PEARS licence contacts for licence {} because the Energy Portal holds no such licence, " +
              "correlationId: {}",
          licenceId, correlationId);
      return;
    }

    joiningLicenseeContacts(message).forEach(contact -> updateContact(licence.get(), contact, correlationId));
  }

  private void updateContact(Licence licence, JoiningLicenseeContact contact, String correlationId) {
    var organisationUnitId = parseId(contact.joiningLicenseeOuId());

    if (organisationUnitId == null) {
      LOGGER.warn("Ignoring PEARS licence contact with unusable organisation unit id {} for licence {}, correlationId: {}",
          contact.joiningLicenseeOuId(), licence.getId(), correlationId);
      return;
    }

    var contactEmail = contact.joiningLicenseeContactEmail();

    if (StringUtils.isBlank(contactEmail)) {
      LOGGER.warn(
          "Ignoring PEARS licence contact with no email address for licence {} and organisation unit {}, correlationId: {}",
          licence.getId(), organisationUnitId, correlationId);
      return;
    }

    var licensee = licenceResponsibleOrganisationRepository
        .findByLicence_IdAndResponsibleOrganisationId(licence.getId(), organisationUnitId)
        .orElseGet(() -> createLicensee(licence, organisationUnitId, correlationId));

    var licenceContact = licenceContactRepository.findByLicensee(licensee).orElseGet(LicenceContact::new);
    licenceContact.setLicensee(licensee);
    licenceContact.setContactEmail(contactEmail);
    licenceContactRepository.save(licenceContact);

    LOGGER.info("Updated PEARS licence contact for licence {} and organisation unit {}, correlationId: {}",
        licence.getId(), organisationUnitId, correlationId);
  }

  /**
   * Creates the licensee the contact hangs off, for the case where PEARS names a joining licensee that the Energy
   * Portal's own view of the licence has not caught up with, and so was not created by the refresh this message
   * triggered. The row is created unmanaged, exactly as the refresh creates it, so the refresh stays in charge of
   * removing it again if the Energy Portal continues to disagree.
   */
  private LicenceResponsibleOrganisation createLicensee(Licence licence, Integer organisationUnitId, String correlationId) {
    LOGGER.info(
        "Creating licensee for licence {} and organisation unit {} named by PEARS licence contacts, correlationId: {}",
        licence.getId(), organisationUnitId, correlationId);

    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(licence);
    licensee.setResponsibleOrganisationId(organisationUnitId);
    licensee.setManagedByLms(false);
    return licenceResponsibleOrganisationRepository.save(licensee);
  }

  private List<JoiningLicenseeContact> joiningLicenseeContacts(PearsLicenceContactsEpmqMessage message) {
    return Optional.ofNullable(message.getContacts())
        .map(PearsLicenceContacts::joiningLicenseeContacts)
        .orElse(List.of());
  }

  private Integer parseId(String id) {
    if (StringUtils.isBlank(id)) {
      return null;
    }

    try {
      return Integer.valueOf(id.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
