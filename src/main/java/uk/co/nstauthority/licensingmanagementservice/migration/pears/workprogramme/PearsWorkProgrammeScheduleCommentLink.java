package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.MigrationEntity;

/**
 * A schedule comment the migration wrote, so the cleardown removes it and leaves any added in LMS.
 */
@Entity
@MigrationEntity
@Table(name = "pears_work_programme_schedule_comment_links")
public class PearsWorkProgrammeScheduleCommentLink {

  @Id
  private UUID scheduleCommentId;

  protected PearsWorkProgrammeScheduleCommentLink() {
  }

  PearsWorkProgrammeScheduleCommentLink(UUID scheduleCommentId) {
    this.scheduleCommentId = scheduleCommentId;
  }

  public UUID getScheduleCommentId() {
    return scheduleCommentId;
  }
}
