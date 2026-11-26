package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;

/**
 * Writes and clears a migrated schedule for tests outside this package, through the schedule migration's own writer
 * and cleardown. Import it into an integration test with {@code @Import}.
 */
public class MigratedScheduleFixture {

  private final PearsScheduleWriter pearsScheduleWriter;
  private final PearsScheduleCleardownService pearsScheduleCleardownService;

  MigratedScheduleFixture(
      PearsScheduleWriter pearsScheduleWriter,
      PearsScheduleMigratedLicenceRepository migratedLicenceRepository,
      EntityManager entityManager
  ) {
    this.pearsScheduleWriter = pearsScheduleWriter;
    this.pearsScheduleCleardownService = new PearsScheduleCleardownService(migratedLicenceRepository, entityManager);
  }

  /**
   * Migrates the P2244-shaped schedule of {@link PearsScheduleWriterIntegrationTest#pearsSchedule()} onto the licence:
   * start 1 December 2014, three terms, and a {@code DD} event on 1 December 2019, in the second term.
   */
  public LicenceSchedule migrate(Licence licence) {
    var pearsSchedule = PearsScheduleWriterIntegrationTest.pearsSchedule();
    var mappedSchedule = new PearsScheduleMapper().map(pearsSchedule, PearsScheduleWriterIntegrationTest.EVENT_TYPES);
    return pearsScheduleWriter.write(licence, mappedSchedule, pearsSchedule, UUID.randomUUID()).orElseThrow();
  }

  /**
   * Deletes the licence's migrated schedule, in the caller's transaction.
   */
  public void clear(Licence licence) {
    pearsScheduleCleardownService.clear(licence.getLicenceReference());
  }

  /**
   * The schedule cleardown, which only exists under the {@code migration} profile otherwise.
   */
  public PearsScheduleCleardownService cleardownService() {
    return pearsScheduleCleardownService;
  }
}
