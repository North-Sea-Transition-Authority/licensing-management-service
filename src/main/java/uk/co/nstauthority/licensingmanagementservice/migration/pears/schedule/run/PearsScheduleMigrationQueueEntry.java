package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.MigrationEntity;

/**
 * One licence in a run.
 */
@Entity
@MigrationEntity
@Table(name = "pears_schedule_migration_queue")
public class PearsScheduleMigrationQueueEntry {

  @EmbeddedId
  private PearsScheduleMigrationQueueEntryId id;

  @Enumerated(EnumType.STRING)
  private PearsScheduleMigrationQueueStatus status;

  private int attempts;

  /**
   * Why the licence was skipped or failed.
   */
  private String outcome;

  /**
   * The mapper's migration notes, counted by reason.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Integer> notes;

  private Instant startedAt;

  private Instant finishedAt;

  protected PearsScheduleMigrationQueueEntry() {
  }

  PearsScheduleMigrationQueueEntry(PearsScheduleMigrationQueueEntryId id) {
    this.id = id;
    this.status = PearsScheduleMigrationQueueStatus.PENDING;
  }

  public PearsScheduleMigrationQueueEntryId getId() {
    return id;
  }

  public PearsScheduleMigrationQueueStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getOutcome() {
    return outcome;
  }

  public Map<String, Integer> getNotes() {
    return notes;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  void recordOutcome(
      PearsScheduleMigrationQueueStatus result,
      String outcome,
      Map<String, Integer> notes,
      Instant startedAt,
      Instant finishedAt
  ) {
    this.status = result;
    this.outcome = outcome;
    this.notes = notes;
    this.startedAt = startedAt;
    this.finishedAt = finishedAt;
    this.attempts++;
  }
}
