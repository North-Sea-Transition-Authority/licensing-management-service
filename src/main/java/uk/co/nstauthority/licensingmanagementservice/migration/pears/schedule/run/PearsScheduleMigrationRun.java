package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.MigrationEntity;

@Entity
@MigrationEntity
@Table(name = "pears_schedule_migration_runs")
public class PearsScheduleMigrationRun {

  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  private PearsScheduleMigrationRunKind kind;

  @Enumerated(EnumType.STRING)
  private PearsScheduleMigrationRunStatus status;

  private long requestedBy;

  private Instant requestedAt;

  private Instant completedAt;

  protected PearsScheduleMigrationRun() {
  }

  PearsScheduleMigrationRun(UUID id, PearsScheduleMigrationRunKind kind, long requestedBy, Instant requestedAt) {
    this.id = id;
    this.kind = kind;
    this.status = PearsScheduleMigrationRunStatus.QUEUED;
    this.requestedBy = requestedBy;
    this.requestedAt = requestedAt;
  }

  public UUID getId() {
    return id;
  }

  public PearsScheduleMigrationRunKind getKind() {
    return kind;
  }

  public PearsScheduleMigrationRunStatus getStatus() {
    return status;
  }

  public long getRequestedBy() {
    return requestedBy;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  void start() {
    status = PearsScheduleMigrationRunStatus.RUNNING;
  }

  void finish(PearsScheduleMigrationRunStatus finishedStatus, Instant at) {
    status = finishedStatus;
    completedAt = at;
  }

  void reopen() {
    status = PearsScheduleMigrationRunStatus.RUNNING;
    completedAt = null;
  }
}
