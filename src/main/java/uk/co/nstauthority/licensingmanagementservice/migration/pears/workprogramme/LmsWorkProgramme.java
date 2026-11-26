package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;

/**
 * A licence's work programme as LMS stores it on its schedule's {@code ACTIVE} detail, for the comparison report.
 */
record LmsWorkProgramme(List<Activity> activities, List<ScheduleComment> scheduleComments) {

  LmsWorkProgramme {
    activities = List.copyOf(activities);
    scheduleComments = List.copyOf(scheduleComments);
  }

  static LmsWorkProgramme none() {
    return new LmsWorkProgramme(List.of(), List.of());
  }

  /**
   * One activity.
   *
   * @param pedWpId         the PEARS programme it was migrated from; null for an activity added in LMS
   * @param pearsActivityId the PEARS activity it was migrated from; null for an activity added in LMS
   * @param statuses        its status history, oldest first
   * @param comments        its published comments' text, oldest first
   */
  record Activity(
      UUID id,
      @Nullable Long pedWpId,
      @Nullable String pearsActivityId,
      WorkProgrammeActivityCategory category,
      @Nullable String otherCategoryName,
      @Nullable WorkProgrammeActivityCommitment commitment,
      @Nullable String description,
      WorkProgrammeActivityDateOption dateOption,
      @Nullable TermType termType,
      @Nullable PhaseType phaseType,
      @Nullable ThreeFieldDuration relativeDuration,
      @Nullable LocalDate dueDate,
      List<Status> statuses,
      List<String> comments
  ) {

    Activity {
      statuses = List.copyOf(statuses);
      comments = List.copyOf(comments);
    }
  }

  record Status(WorkProgrammeStatus status, Instant appliedAt) {
  }

  /**
   * A schedule comment.
   *
   * @param migrated whether the migration wrote it, rather than someone in LMS
   */
  record ScheduleComment(String text, boolean migrated) {
  }
}
