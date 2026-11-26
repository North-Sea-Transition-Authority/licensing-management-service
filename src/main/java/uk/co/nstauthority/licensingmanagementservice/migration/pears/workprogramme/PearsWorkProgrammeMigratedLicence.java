package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.MigrationEntity;

/**
 * Marks a migrated schedule as having had its work programme migrated, even one with no activities to migrate, so the
 * licence isn't migrated twice.
 */
@Entity
@MigrationEntity
@Table(name = "pears_work_programme_migrated_licences")
public class PearsWorkProgrammeMigratedLicence {

  @Id
  private UUID licenceScheduleId;

  private int licenceId;

  private UUID runId;

  private Instant migratedAt;

  protected PearsWorkProgrammeMigratedLicence() {
  }

  PearsWorkProgrammeMigratedLicence(UUID licenceScheduleId, int licenceId, UUID runId, Instant migratedAt) {
    this.licenceScheduleId = licenceScheduleId;
    this.licenceId = licenceId;
    this.runId = runId;
    this.migratedAt = migratedAt;
  }

  public UUID getLicenceScheduleId() {
    return licenceScheduleId;
  }

  public int getLicenceId() {
    return licenceId;
  }

  public UUID getRunId() {
    return runId;
  }

  public Instant getMigratedAt() {
    return migratedAt;
  }
}
