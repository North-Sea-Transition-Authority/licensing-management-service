package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.MigrationEntity;

/**
 * The PEARS event a migrated schedule event came from, so the comparison report pairs them exactly. Under rate option A
 * one PEARS rate can be the source of several LMS rates, each with its own link.
 */
@Entity
@MigrationEntity
@Table(name = "pears_schedule_event_migration_links")
public class PearsScheduleEventMigrationLink {

  @Id
  private UUID scheduleEventId;

  private long pearsPlsdId;

  private String pearsEventMnem;

  private int pearsEventIndex;

  private String pearsEventId;

  protected PearsScheduleEventMigrationLink() {
  }

  PearsScheduleEventMigrationLink(UUID scheduleEventId, long pearsPlsdId, PearsScheduleEvent source) {
    this.scheduleEventId = scheduleEventId;
    this.pearsPlsdId = pearsPlsdId;
    this.pearsEventMnem = source.eventMnem();
    this.pearsEventIndex = source.index();
    this.pearsEventId = source.eventId();
  }

  public UUID getScheduleEventId() {
    return scheduleEventId;
  }

  public long getPearsPlsdId() {
    return pearsPlsdId;
  }

  public String getPearsEventMnem() {
    return pearsEventMnem;
  }

  public int getPearsEventIndex() {
    return pearsEventIndex;
  }

  public String getPearsEventId() {
    return pearsEventId;
  }
}
