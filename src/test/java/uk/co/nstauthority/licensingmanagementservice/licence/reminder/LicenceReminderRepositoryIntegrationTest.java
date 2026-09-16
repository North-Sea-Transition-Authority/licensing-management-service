package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

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
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class LicenceReminderRepositoryIntegrationTest {

  private static final Integer ORGANISATION_ID = 181;
  private static final LocalDate DEADLINE = LocalDate.of(2026, Month.AUGUST, 31);

  @Autowired
  private LicenceReminderRepository licenceReminderRepository;

  @Autowired
  private EntityManager em;

  private Licence licence;
  private LicenceScheduleTerm term;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceReference("P001")
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    em.persist(licence);

    term = persistTerm();
  }

  @Test
  void save_persistsAndReadsBackTheReminder() {
    var reminder = licenceReminderRepository.save(buildReminder(UUID.randomUUID()));
    em.flush();
    em.clear();

    var persisted = licenceReminderRepository.findById(reminder.getId()).orElseThrow();

    assertThat(persisted)
        .extracting(
            LicenceReminder::getOriginalEventId,
            LicenceReminder::getResponsibleOrganisationId,
            LicenceReminder::getDeadlineDate,
            LicenceReminder::getNoticePeriod,
            LicenceReminder::getNotificationBatchReference)
        .containsExactly(
            reminder.getOriginalEventId(),
            ORGANISATION_ID,
            DEADLINE,
            NoticePeriod.SIX_MONTHS,
            reminder.getNotificationBatchReference());
  }

  @Test
  void findAllByLicenceIn_returnsWhatWasAlreadyQueued() {
    var reminder = licenceReminderRepository.save(buildReminder(UUID.randomUUID()));
    em.flush();

    assertThat(licenceReminderRepository.findAllByLicenceIn(List.of(licence)))
        .extracting(
            LicenceReminder::getOriginalEventId,
            LicenceReminder::getResponsibleOrganisationId,
            LicenceReminder::getDeadlineDate)
        .containsExactly(tuple(reminder.getOriginalEventId(), ORGANISATION_ID, DEADLINE));
  }

  @Test
  void findAllByLicenceIn_whenTheLicenceDiffers_thenNothingIsFound() {
    var otherLicence = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceReference("P002")
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    em.persist(otherLicence);
    var reminder = licenceReminderRepository.save(buildReminder(UUID.randomUUID()));
    em.flush();

    assertThat(licenceReminderRepository.findAllByLicenceIn(List.of(otherLicence)))
        .isEmpty();
    assertThat(reminder.getNoticePeriod()).isEqualTo(NoticePeriod.SIX_MONTHS);
  }

  @Test
  void save_whenTheReminderIsLicenceLevelWithNoScheduleEvent_thenItPersists() {
    var reminder = buildReminder(null);
    reminder.setScheduleEvent(null);
    reminder.setReminderType(ReminderType.LICENCE_EXPIRY);
    licenceReminderRepository.save(reminder);
    em.flush();

    assertThat(licenceReminderRepository.findAllByLicenceIn(List.of(licence)))
        .extracting(LicenceReminder::getReminderType, LicenceReminder::getOriginalEventId)
        .containsExactly(tuple(ReminderType.LICENCE_EXPIRY, null));
  }

  @Test
  void save_whenTheSameLicenceLevelReminderIsQueuedTwice_thenRejected() {
    var first = buildReminder(null);
    first.setScheduleEvent(null);
    first.setReminderType(ReminderType.LICENCE_EXPIRY);
    licenceReminderRepository.save(first);
    em.flush();

    var second = buildReminder(null);
    second.setScheduleEvent(null);
    second.setReminderType(ReminderType.LICENCE_EXPIRY);
    licenceReminderRepository.save(second);

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(em::flush);
  }

  @Test
  void save_whenTheSameDeadlineOrganisationNoticePeriodAndDateIsQueuedTwice_thenRejected() {
    var originalEventId = UUID.randomUUID();
    licenceReminderRepository.save(buildReminder(originalEventId));
    em.flush();

    licenceReminderRepository.save(buildReminder(originalEventId));

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(em::flush);
  }

  @Test
  void save_whenTheScheduleWasEditedSoTheEventIdChanged_thenStillRejectedOnOriginalEventId() {
    var originalEventId = UUID.randomUUID();
    licenceReminderRepository.save(buildReminder(originalEventId));
    em.flush();

    var duplicatedTerm = persistTerm();
    var afterEdit = buildReminder(originalEventId);
    afterEdit.setScheduleEvent(duplicatedTerm);
    licenceReminderRepository.save(afterEdit);

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(em::flush);
  }

  private LicenceReminder buildReminder(UUID originalEventId) {
    var reminder = new LicenceReminder();
    reminder.setScheduleEvent(term);
    reminder.setOriginalEventId(originalEventId);
    reminder.setReminderType(ReminderType.TERM_OR_PHASE_END);
    reminder.setLicence(licence);
    reminder.setResponsibleOrganisationId(ORGANISATION_ID);
    reminder.setDeadlineDate(DEADLINE);
    reminder.setNoticePeriod(NoticePeriod.SIX_MONTHS);
    reminder.setNotificationBatchReference(UUID.randomUUID());
    reminder.setQueuedAt(Instant.parse("2026-02-28T07:00:00Z"));
    return reminder;
  }

  private LicenceScheduleTerm persistTerm() {
    var persistedTerm = LicenceScheduleTermTestUtilFactory.build(persistScheduleDetail());
    em.persist(persistedTerm);
    return persistedTerm;
  }

  private LicenceScheduleDetail persistScheduleDetail() {
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    em.persist(licenceSchedule);

    var licenceScheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();
    em.persist(licenceScheduleDetail);

    return licenceScheduleDetail;
  }

  private static final class LicenceScheduleTermTestUtilFactory {
    private LicenceScheduleTermTestUtilFactory() {
    }

    private static LicenceScheduleTerm build(LicenceScheduleDetail licenceScheduleDetail) {
      var term = new LicenceScheduleTerm();
      term.setLicenceScheduleDetail(licenceScheduleDetail);
      term.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
      term.setTermType(TermType.INITIAL);
      term.setStartDate(LocalDate.of(2022, Month.SEPTEMBER, 1));
      term.setEndDate(DEADLINE);
      return term;
    }
  }
}
