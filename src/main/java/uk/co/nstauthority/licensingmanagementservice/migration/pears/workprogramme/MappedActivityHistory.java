package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A migrated activity's status history, oldest first, and its comments. Times are PEARS's, which are UK local time.
 */
public record MappedActivityHistory(List<Status> statuses, List<Comment> comments) {

  public MappedActivityHistory {
    statuses = List.copyOf(statuses);
    comments = List.copyOf(comments);
  }

  /**
   * One status in the history.
   *
   * @param pearsStatus the PEARS status it came from, for the report
   */
  public record Status(MigratedStatus status, LocalDateTime appliedAt, String pearsStatus) {
  }

  /**
   * A comment as it will be published, with the PEARS author's name in the text, as LMS has no user to attach it to.
   */
  public record Comment(String text, LocalDateTime timestamp) {
  }
}
