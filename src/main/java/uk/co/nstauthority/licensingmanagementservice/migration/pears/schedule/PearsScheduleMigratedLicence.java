package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.MigrationEntity;

/**
 * Marks a licence schedule as created by the PEARS schedule migration, which is what the cleardown may delete.
 */
@Entity
@MigrationEntity
@Table(name = "pears_schedule_migrated_licences")
public class PearsScheduleMigratedLicence {

  @Id
  private UUID licenceScheduleId;

  private int licenceId;

  private UUID runId;

  private Instant migratedAt;

  protected PearsScheduleMigratedLicence() {
  }

  PearsScheduleMigratedLicence(UUID licenceScheduleId, int licenceId, UUID runId, Instant migratedAt) {
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
