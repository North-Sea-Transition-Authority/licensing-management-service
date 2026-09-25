package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class LicenceReminderBounceRepositoryIntegrationTest {

  private static final Instant REPORTED_AT = Instant.parse("2026-09-17T08:00:00Z");

  @Autowired
  private LicenceReminderBounceRepository licenceReminderBounceRepository;

  @Autowired
  private EntityManager em;

  @Test
  void save_persistsAndReadsBackTheBounce() {
    var batchReference = UUID.randomUUID();
    var bounce = licenceReminderBounceRepository.save(buildBounce(batchReference));
    em.flush();
    em.clear();

    var persisted = licenceReminderBounceRepository.findById(bounce.getId()).orElseThrow();

    assertThat(persisted)
        .extracting(LicenceReminderBounce::getNotificationBatchReference, LicenceReminderBounce::getReportedAt)
        .containsExactly(batchReference, REPORTED_AT);
  }

  @Test
  void save_whenTheSameBouncedEmailIsRecordedTwice_thenRejected() {
    var batchReference = UUID.randomUUID();
    licenceReminderBounceRepository.save(buildBounce(batchReference));
    em.flush();

    licenceReminderBounceRepository.save(buildBounce(batchReference));

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(em::flush);
  }

  private LicenceReminderBounce buildBounce(UUID batchReference) {
    var bounce = new LicenceReminderBounce();
    bounce.setNotificationBatchReference(batchReference);
    bounce.setReportedAt(REPORTED_AT);
    return bounce;
  }
}
