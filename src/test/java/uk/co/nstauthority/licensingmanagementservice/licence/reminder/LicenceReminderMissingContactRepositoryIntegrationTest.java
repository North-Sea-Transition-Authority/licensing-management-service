package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class LicenceReminderMissingContactRepositoryIntegrationTest {

  private static final Integer ORGANISATION_ID = 181;
  private static final LocalDate DEADLINE = LocalDate.of(2027, Month.MARCH, 31);
  private static final Instant REPORTED_AT = Instant.parse("2026-09-30T07:00:00Z");

  @Autowired
  private LicenceReminderMissingContactRepository licenceReminderMissingContactRepository;

  @Autowired
  private EntityManager em;

  private Licence licence;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(640)
        .withLicenceReference("P640")
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    em.persist(licence);
  }

  @Test
  void save_persistsAndReadsBackTheMissingContact() {
    var originalEventId = UUID.randomUUID();
    var saved = licenceReminderMissingContactRepository.save(buildMissingContact(originalEventId));
    em.flush();
    em.clear();

    var persisted = licenceReminderMissingContactRepository.findById(saved.getId()).orElseThrow();

    assertThat(persisted)
        .extracting(
            missingContact -> missingContact.getLicence().getId(),
            LicenceReminderMissingContact::getResponsibleOrganisationId,
            LicenceReminderMissingContact::getOriginalEventId,
            LicenceReminderMissingContact::getReminderType,
            LicenceReminderMissingContact::getDeadlineDate,
            LicenceReminderMissingContact::getReportedAt)
        .containsExactly(
            licence.getId(),
            ORGANISATION_ID,
            originalEventId,
            ReminderType.TERM_OR_PHASE_END,
            DEADLINE,
            REPORTED_AT);
  }

  @Test
  void findAllByLicenceIn_returnsOnlyTheRecordsForThoseLicences() {
    var missingContact = licenceReminderMissingContactRepository.save(buildMissingContact(UUID.randomUUID()));
    em.flush();

    assertThat(licenceReminderMissingContactRepository.findAllByLicenceIn(List.of(licence)))
        .extracting(LicenceReminderMissingContact::getId)
        .containsExactly(missingContact.getId());
  }

  @Test
  void save_whenTheSameDeadlineAndLicenseeIsRecordedTwice_thenRejected() {
    var originalEventId = UUID.randomUUID();
    licenceReminderMissingContactRepository.save(buildMissingContact(originalEventId));
    em.flush();

    licenceReminderMissingContactRepository.save(buildMissingContact(originalEventId));

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(em::flush);
  }

  @Test
  void save_whenTheDeadlineIsALicenceExpiry_thenItPersistsWithNoEventId() {
    var expiry = buildMissingContact(null);
    expiry.setReminderType(ReminderType.LICENCE_EXPIRY);
    var saved = licenceReminderMissingContactRepository.save(expiry);
    em.flush();
    em.clear();

    assertThat(licenceReminderMissingContactRepository.findById(saved.getId()).orElseThrow())
        .extracting(
            LicenceReminderMissingContact::getOriginalEventId,
            LicenceReminderMissingContact::getReminderType)
        .containsExactly(null, ReminderType.LICENCE_EXPIRY);
  }

  @Test
  void save_whenTheSameLicenceExpiryIsRecordedTwice_thenRejected() {
    var first = buildMissingContact(null);
    first.setReminderType(ReminderType.LICENCE_EXPIRY);
    licenceReminderMissingContactRepository.save(first);
    em.flush();

    var second = buildMissingContact(null);
    second.setReminderType(ReminderType.LICENCE_EXPIRY);
    licenceReminderMissingContactRepository.save(second);

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(em::flush);
  }

  private LicenceReminderMissingContact buildMissingContact(UUID originalEventId) {
    var missingContact = new LicenceReminderMissingContact();
    missingContact.setLicence(licence);
    missingContact.setResponsibleOrganisationId(ORGANISATION_ID);
    missingContact.setOriginalEventId(originalEventId);
    missingContact.setReminderType(ReminderType.TERM_OR_PHASE_END);
    missingContact.setDeadlineDate(DEADLINE);
    missingContact.setReportedAt(REPORTED_AT);
    return missingContact;
  }
}
