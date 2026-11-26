package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.MigrationEntity;

/**
 * The PEARS activity a migrated work programme activity came from, so the comparison report pairs them exactly.
 */
@Entity
@MigrationEntity
@Table(name = "pears_work_programme_activity_migration_links")
public class PearsWorkProgrammeActivityMigrationLink {

  @Id
  private UUID scheduleEventId;

  private long pearsWpId;

  private long pearsWpdId;

  private String pearsActivityId;

  private String pearsType;

  protected PearsWorkProgrammeActivityMigrationLink() {
  }

  PearsWorkProgrammeActivityMigrationLink(
      UUID scheduleEventId,
      long pearsWpId,
      long pearsWpdId,
      String pearsActivityId,
      String pearsType
  ) {
    this.scheduleEventId = scheduleEventId;
    this.pearsWpId = pearsWpId;
    this.pearsWpdId = pearsWpdId;
    this.pearsActivityId = pearsActivityId;
    this.pearsType = pearsType;
  }

  public UUID getScheduleEventId() {
    return scheduleEventId;
  }

  public long getPearsWpId() {
    return pearsWpId;
  }

  public long getPearsWpdId() {
    return pearsWpdId;
  }

  public String getPearsActivityId() {
    return pearsActivityId;
  }

  public String getPearsType() {
    return pearsType;
  }
}
