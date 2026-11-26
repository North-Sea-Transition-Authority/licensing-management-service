package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.MappedActivityHistory.Comment;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.MappedActivityHistory.Status;

/**
 * Maps a work programme's statuses and notes, following the plan's M5 (status history) and M6 (notes to comments).
 * <ul>
 *   <li>each activity's status history is rebuilt from its status notes, one row per note, or is one row for its
 *       current status, dated from the first version holding it, where it has none;</li>
 *   <li>a note on a migrated activity becomes a comment on it, with the author's name in the text;</li>
 *   <li>general notes, notes on an activity that isn't migrated, and a breakpoint with no drill-or-drop well go to the
 *       schedule comments area;</li>
 *   <li>notes written by the 2011 bulk job ("Migration Process") aren't migrated.</li>
 * </ul>
 */
final class PearsNoteMapper {

  static final String SYSTEM_NOTE_LEFT_OUT = "Note by the 2011 bulk job (Migration Process): not migrated";
  static final String GENERAL_NOTE = "General note: to the schedule comments area";
  static final String NOTE_ON_ACTIVITY_NOT_MIGRATED = "Note on an activity not migrated: to the schedule comments area";
  static final String STATUS_FROM_NOTES = "Status history from the status notes";
  static final String STATUS_WITHOUT_NOTE = "Status with no status note: dated from the first version holding it";
  static final String LEGACY_STATUS_CARRIED_OVER =
      "Status set by the 2011 bulk job: dated 12/07/2011, with a comment that the real date isn't held";
  static final String CURRENT_STATUS_AFTER_NOTES =
      "Current status isn't the last status note's: the current status added after the notes";
  static final String PART_WAIVER = "PART_WAIVER: IN_PROGRESS, with a comment (provisional, WP-L4)";
  static final String WAIVER_DECLINED = "WAIVER_DECLINED: OPEN, with a comment (provisional, WP-L4)";
  static final String NOTE_WITHOUT_DATE = "Note with no date: dated from the work programme's last save";
  static final String BREAKPOINT_WITHOUT_DRILL_OR_DROP_WELL = "Breakpoint with no drill-or-drop well: a schedule comment";

  static final String SYSTEM_AUTHOR = "Migration Process";
  static final String LEGACY_STATUS_COMMENT =
      "Status carried over from legacy records on 12/07/2011; completion date not held in PEARS";

  private static final LocalDate BULK_JOB_DATE = LocalDate.of(2011, Month.JULY, 12);
  private static final String NOT_STARTED_STATUS = "NOT_STARTED";
  private static final String PART_WAIVER_STATUS = "PART_WAIVER";
  private static final String WAIVER_DECLINED_STATUS = "WAIVER_DECLINED";
  private static final Set<String> WAIVER_STATUSES = Set.of(PART_WAIVER_STATUS, WAIVER_DECLINED_STATUS);
  private static final Set<String> KNOWN_STATUSES = Set.of(
      NOT_STARTED_STATUS, "IN_PROGRESS", "COMPLETE", "FULL_WAIVER", "DROPPED", PART_WAIVER_STATUS, WAIVER_DECLINED_STATUS);

  private PearsNoteMapper() {
  }

  /**
   * The programme's statuses and notes.
   *
   * @param activities the programme's activities as {@link PearsActivityMapper} mapped them
   * @throws PearsWorkProgrammeMappingException if an activity or note has a status the migration doesn't know
   */
  static MappedNotes notes(PearsWorkProgramme workProgramme, MappedActivities activities, MigrationNotes notes) {
    requireKnownStatuses(workProgramme);

    var migrated = activities.activities().stream()
        .collect(Collectors.toMap(MappedActivity::pearsActivityId, activity -> activity));

    var notesByActivity = new HashMap<String, List<PearsNote>>();
    var scheduleComments = new ArrayList<Comment>();

    for (var note : workProgramme.notes()) {
      var activityId = StringUtils.strip(note.activityId());
      if (SYSTEM_AUTHOR.equals(StringUtils.strip(note.createdBy()))) {
        notes.note(SYSTEM_NOTE_LEFT_OUT);
        continue;
      }
      if (note.datetime() == null) {
        notes.note(NOTE_WITHOUT_DATE);
      }
      if (StringUtils.isEmpty(activityId)) {
        notes.note(GENERAL_NOTE);
        scheduleComments.add(new Comment(noteText(note, null), noteTime(workProgramme, note)));
      } else if (!migrated.containsKey(activityId)) {
        notes.note(NOTE_ON_ACTIVITY_NOT_MIGRATED);
        scheduleComments.add(new Comment(noteText(note, note.activityTitle()), noteTime(workProgramme, note)));
      } else {
        notesByActivity.computeIfAbsent(activityId, id -> new ArrayList<>()).add(note);
      }
    }

    breakpointComment(workProgramme, activities, notes).ifPresent(scheduleComments::add);

    var histories = new HashMap<String, MappedActivityHistory>();
    for (var activity : activities.activities()) {
      histories.put(activity.pearsActivityId(), history(
          workProgramme, activity, notesByActivity.getOrDefault(activity.pearsActivityId(), List.of()), notes));
    }

    scheduleComments.sort(Comparator.comparing(Comment::timestamp));
    return new MappedNotes(histories, scheduleComments);
  }

  private static MappedActivityHistory history(
      PearsWorkProgramme workProgramme,
      MappedActivity activity,
      List<PearsNote> activityNotes,
      MigrationNotes notes
  ) {
    var source = activity.source();
    var statusSince = source.statusSince() != null ? source.statusSince() : workProgramme.start();

    var statusNotes = activityNotes.stream()
        .filter(note -> StringUtils.isNotBlank(note.newActivityStatus()))
        .sorted(Comparator.comparing(note -> noteTime(workProgramme, note)))
        .toList();
    var statuses = statusNotes.isEmpty()
        ? List.of(status(workProgramme, source, StringUtils.strip(source.status()), statusSince, notes))
        : statusesFromNotes(workProgramme, source, statusNotes, statusSince, notes);

    var comments = new ArrayList<Comment>();
    if (statusNotes.isEmpty() && !NOT_STARTED_STATUS.equals(StringUtils.strip(source.status()))) {
      notes.note(STATUS_WITHOUT_NOTE);
      if (isSetByBulkJob(source, statusSince)) {
        notes.note(LEGACY_STATUS_CARRIED_OVER);
        comments.add(new Comment(LEGACY_STATUS_COMMENT, statusSince));
      }
    }

    for (var status : statuses) {
      if (WAIVER_STATUSES.contains(status.pearsStatus())) {
        comments.add(new Comment("PEARS status: %s, which LMS doesn't have".formatted(status.pearsStatus()),
            status.appliedAt()));
      }
    }
    if (activity.stub()) {
      comments.add(new Comment(PearsActivityMapper.STUB_COMMENT, statusSince));
    }
    for (var note : activityNotes) {
      comments.add(new Comment(noteText(note, null), noteTime(workProgramme, note)));
    }

    comments.sort(Comparator.comparing(Comment::timestamp));
    return new MappedActivityHistory(statuses, comments);
  }

  /**
   * One status per status note, oldest first. If the current status isn't the last note's, it is added after them, as
   * the notes are what PEARS shows as the history but the status is what PEARS holds now.
   */
  private static List<Status> statusesFromNotes(
      PearsWorkProgramme workProgramme,
      PearsActivity source,
      List<PearsNote> statusNotes,
      LocalDateTime statusSince,
      MigrationNotes notes
  ) {
    notes.note(STATUS_FROM_NOTES);
    var statuses = new ArrayList<Status>();
    for (var note : statusNotes) {
      statuses.add(status(workProgramme, source, StringUtils.strip(note.newActivityStatus()),
          noteTime(workProgramme, note), notes));
    }

    var currentStatus = StringUtils.strip(source.status());
    if (!statusNotes.getLast().newActivityStatus().strip().equals(currentStatus)) {
      notes.note(CURRENT_STATUS_AFTER_NOTES);
      var lastNoteTime = statuses.getLast().appliedAt();
      statuses.add(status(workProgramme, source, currentStatus,
          statusSince.isAfter(lastNoteTime) ? statusSince : lastNoteTime, notes));
    }
    return statuses;
  }

  /**
   * A {@code LEGACY} activity the 2011 bulk job created with its status already set, carried over from pre-2010
   * records (WP-S9e): its fallback date is the job's, not when the work was done.
   */
  private static boolean isSetByBulkJob(PearsActivity source, LocalDateTime statusSince) {
    return "LEGACY".equals(StringUtils.strip(source.activityType())) && statusSince.toLocalDate().equals(BULK_JOB_DATE);
  }

  private static Status status(
      PearsWorkProgramme workProgramme,
      PearsActivity activity,
      @Nullable String pearsStatus,
      LocalDateTime appliedAt,
      MigrationNotes notes
  ) {
    var status = switch (StringUtils.defaultString(pearsStatus)) {
      case NOT_STARTED_STATUS -> MigratedStatus.OPEN;
      case "IN_PROGRESS" -> MigratedStatus.IN_PROGRESS;
      case "COMPLETE" -> MigratedStatus.COMPLETE;
      case "FULL_WAIVER" -> MigratedStatus.FULL_WAIVER;
      case "DROPPED" -> MigratedStatus.DROPPED;
      case PART_WAIVER_STATUS -> {
        notes.note(PART_WAIVER);
        yield MigratedStatus.IN_PROGRESS;
      }
      case WAIVER_DECLINED_STATUS -> {
        notes.note(WAIVER_DECLINED);
        yield MigratedStatus.OPEN;
      }
      default -> throw new PearsWorkProgrammeMappingException(workProgramme.licence(),
          "Activity %s has the status '%s', which the migration doesn't know"
              .formatted(activity.activityId(), pearsStatus));
    };
    return new Status(status, appliedAt, pearsStatus);
  }

  /**
   * The note's text, with the PEARS author's name after it, as LMS has no user to attach it to. A note on an activity
   * that isn't migrated is headed with that activity's title, so it still reads sensibly on its own.
   */
  private static String noteText(PearsNote note, @Nullable String activityTitle) {
    var text = StringUtils.strip(note.description());
    if (StringUtils.isEmpty(text)) {
      text = StringUtils.isNotBlank(note.newActivityStatus())
          ? "Status changed to " + note.newActivityStatus().strip()
          : "(no text)";
    }
    if (StringUtils.isNotBlank(activityTitle)) {
      text = activityTitle.strip() + ": " + text;
    }
    var author = StringUtils.strip(note.createdBy());
    return StringUtils.isEmpty(author) ? text + " (PEARS note)" : "%s (PEARS note by %s)".formatted(text, author);
  }

  private static LocalDateTime noteTime(PearsWorkProgramme workProgramme, PearsNote note) {
    return note.datetime() != null ? note.datetime() : workProgramme.start();
  }

  /**
   * Fails the licence on any status the migration doesn't know: every activity's, migrated or not, and every status
   * note's apart from the 2011 bulk job's, before anything is skipped or sent to the schedule comments.
   */
  private static void requireKnownStatuses(PearsWorkProgramme workProgramme) {
    for (var activity : workProgramme.activities()) {
      requireKnownStatus(workProgramme, activity.activityId(), activity.status());
    }
    for (var note : workProgramme.notes()) {
      if (StringUtils.isNotBlank(note.newActivityStatus()) && !SYSTEM_AUTHOR.equals(StringUtils.strip(note.createdBy()))) {
        requireKnownStatus(workProgramme, note.activityId(), note.newActivityStatus());
      }
    }
  }

  private static void requireKnownStatus(PearsWorkProgramme workProgramme, @Nullable String activityId,
                                         @Nullable String pearsStatus) {
    if (!KNOWN_STATUSES.contains(StringUtils.strip(pearsStatus))) {
      throw new PearsWorkProgrammeMappingException(workProgramme.licence(),
          "Activity %s has the status '%s', which the migration doesn't know".formatted(activityId, pearsStatus));
    }
  }

  /**
   * A breakpoint on a programme with no migrated drill-or-drop well has no activity to date (D13), so it is kept as a
   * schedule comment, as it feeds the licence documents. A drill-or-drop well that isn't migrated ("None") doesn't
   * take the date, so doesn't count.
   */
  private static Optional<Comment> breakpointComment(
      PearsWorkProgramme workProgramme,
      MappedActivities activities,
      MigrationNotes notes
  ) {
    var breakpoint = workProgramme.breakpoint();
    var hasDrillOrDropWell = activities.activities().stream()
        .anyMatch(activity -> "DRILL_OR_DROP_WELL".equals(StringUtils.strip(activity.source().activityType())));
    if (breakpoint == null || hasDrillOrDropWell) {
      return Optional.empty();
    }

    var parts = new ArrayList<String>();
    addPart(parts, breakpoint.years(), "year");
    addPart(parts, breakpoint.months(), "month");
    addPart(parts, breakpoint.days(), "day");
    if (parts.isEmpty()) {
      return Optional.empty();
    }

    notes.note(BREAKPOINT_WITHOUT_DRILL_OR_DROP_WELL);
    return Optional.of(new Comment(
        "Drill-or-drop breakpoint: %s from licence start (PEARS work programme)".formatted(String.join(", ", parts)),
        workProgramme.start()));
  }

  private static void addPart(List<String> parts, @Nullable String value, String unit) {
    var stripped = StringUtils.strip(value);
    if (StringUtils.isEmpty(stripped) || "0".equals(stripped)) {
      return;
    }
    parts.add("%s %s%s".formatted(stripped, unit, "1".equals(stripped) ? "" : "s"));
  }
}
