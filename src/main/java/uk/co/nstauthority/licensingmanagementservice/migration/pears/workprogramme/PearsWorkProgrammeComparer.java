package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.CommentRow;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Row;

/**
 * Pairs a licence's PEARS activities with the LMS activities they became, through the migration's link rows, and
 * compares each with what the mapper makes of it. Pure code, so the report, the dry run and the validation sweep can't
 * disagree.
 */
final class PearsWorkProgrammeComparer {

  static final String NOT_MAPPED = "The migration doesn't migrate the licence";

  private PearsWorkProgrammeComparer() {
  }

  /**
   * The comparison.
   *
   * @param workProgrammes   the licence's current PEARS programmes
   * @param mapped           what the mapper makes of them; null when the migration fails or skips the licence
   * @param migrationFailure why it does, when {@code mapped} is null
   */
  static PearsWorkProgrammeComparison compare(
      String licenceReference,
      List<PearsWorkProgramme> workProgrammes,
      @Nullable MappedWorkProgramme mapped,
      @Nullable String migrationFailure,
      LmsWorkProgramme lms
  ) {
    var unpaired = new LinkedHashMap<String, LmsWorkProgramme.Activity>();
    lms.activities().forEach(activity -> unpaired.put(
        activity.pearsActivityId() == null ? activity.id().toString() : key(activity.pedWpId(), activity.pearsActivityId()),
        activity));

    var expected = new LinkedHashMap<String, MappedWorkProgramme.Activity>();
    var skipped = new LinkedHashMap<PearsActivity, String>();
    if (mapped != null) {
      mapped.activities().forEach(activity ->
          expected.put(key(activity.pedWpId(), activity.activity().pearsActivityId()), activity));
      mapped.skipped().forEach(activity -> skipped.put(activity.source(), activity.reason()));
    }

    var rows = new ArrayList<Row>();
    for (var workProgramme : workProgrammes) {
      for (var pears : workProgramme.activities()) {
        var key = key(workProgramme.pedWpId(), StringUtils.strip(pears.activityId()));
        var lmsActivity = unpaired.remove(key);
        rows.add(row(pears, expected.get(key), skipped.get(pears), mapped == null, lmsActivity));
      }
    }
    unpaired.values().forEach(lmsActivity -> rows.add(lmsOnly(lmsActivity)));

    var scheduleComments = mapped == null
        ? lmsOnlyComments(lms)
        : scheduleComments(mapped.scheduleComments(), lms.scheduleComments());
    return new PearsWorkProgrammeComparison(licenceReference, null, migrationFailure, rows, scheduleComments);
  }

  private static Row row(
      PearsActivity pears,
      @Nullable MappedWorkProgramme.Activity expected,
      @Nullable String skippedReason,
      boolean notMapped,
      @Nullable LmsWorkProgramme.Activity lms
  ) {
    if (notMapped || skippedReason != null) {
      var detail = notMapped ? NOT_MAPPED : skippedReason;
      return lms == null
          ? row(pears, null, Result.NOT_MIGRATED, detail)
          : row(pears, lms, Result.DIFFERS, "In LMS, but the migration wouldn't migrate it: " + detail);
    }
    if (lms == null || expected == null) {
      return row(pears, lms, Result.MISSING_IN_LMS, null);
    }

    var differences = differences(expected, lms);
    if (!differences.isEmpty()) {
      return row(pears, lms, Result.DIFFERS, String.join("; ", differences));
    }
    // LMS holds what the mapper makes, so its current status is the mapper's
    var expectedStatuses = expected.history().statuses();
    var lastStatus = expectedStatuses.isEmpty() ? null
        : PearsWorkProgrammeWriter.lmsStatus(expectedStatuses.getLast().status()).name();
    return row(pears, lms, Objects.equals(lastStatus, StringUtils.strip(pears.status())) ? Result.MATCH : Result.MAPPED,
        null);
  }

  private static List<String> differences(MappedWorkProgramme.Activity expected, LmsWorkProgramme.Activity lms) {
    var activity = expected.activity();
    var date = expected.date();
    var differences = new ArrayList<String>();
    differs(differences, "Category", activity.category(), lms.category());
    differs(differences, "Other category name", activity.otherCategoryName(), lms.otherCategoryName());
    differs(differences, "Commitment", activity.commitment(), lms.commitment());
    differs(differences, "Description", activity.description(), lms.description());
    differs(differences, "Date option", date.dateOption(), lms.dateOption());
    differs(differences, "Term", date.termType(), lms.termType());
    differs(differences, "Phase", date.phaseType(), lms.phaseType());
    differs(differences, "Offset", date.relativeDuration(), lms.relativeDuration());
    if (date.dateOption() == WorkProgrammeActivityDateOption.RELATIVE_DATE) {
      differs(differences, "Due date", date.dueDate(), lms.dueDate());
    }

    differs(differences, "Status history", inOrder(lmsStatuses(expected.history())), inOrder(lms.statuses()));
    var expectedComments = expected.history().comments().stream().map(MappedActivityHistory.Comment::text).toList();
    differs(differences, "Comments", sorted(expectedComments), sorted(lms.comments()));
    return differences;
  }

  /**
   * The statuses the writer saves for a mapped history, with the times it gives them.
   */
  static List<LmsWorkProgramme.Status> lmsStatuses(MappedActivityHistory history) {
    var statuses = history.statuses();
    var appliedInstants = PearsWorkProgrammeWriter.appliedInstants(statuses);
    return IntStream.range(0, statuses.size())
        .mapToObj(i -> new LmsWorkProgramme.Status(
            PearsWorkProgrammeWriter.lmsStatus(statuses.get(i).status()), appliedInstants.get(i)))
        .toList();
  }

  /**
   * Statuses by when they were applied, and by name where two were applied at the same moment, which the database can
   * return either way round.
   */
  private static List<LmsWorkProgramme.Status> inOrder(List<LmsWorkProgramme.Status> statuses) {
    return statuses.stream()
        .sorted(Comparator.comparing(LmsWorkProgramme.Status::appliedAt)
            .thenComparing(status -> status.status().name()))
        .toList();
  }

  /**
   * Comments compared as a set of texts: two can share a time, and the database can return those either way round.
   */
  private static List<String> sorted(List<String> comments) {
    return comments.stream().sorted().toList();
  }

  private static void differs(List<String> differences, String what, @Nullable Object expected, @Nullable Object lms) {
    if (!Objects.equals(expected, lms)) {
      differences.add(what + " differs");
    }
  }

  private static Row row(PearsActivity pears, @Nullable LmsWorkProgramme.Activity lms, Result result,
                         @Nullable String detail) {
    return new Row(
        StringUtils.strip(pears.activityId()),
        StringUtils.strip(pears.activityType()),
        pears.title(),
        pears.description(),
        StringUtils.strip(pears.status()),
        lms == null ? null : category(lms),
        lms == null || lms.commitment() == null ? null : lms.commitment().getDisplayName(),
        lms == null ? null : lms.description(),
        lms == null ? null : date(lms),
        lms == null ? null : lastStatus(lms),
        lms == null ? null : "%d".formatted(lms.comments().size()),
        result,
        detail
    );
  }

  private static Row lmsOnly(LmsWorkProgramme.Activity lms) {
    return new Row(null, null, null, null, null, category(lms),
        lms.commitment() == null ? null : lms.commitment().getDisplayName(), lms.description(), date(lms),
        lastStatus(lms), "%d".formatted(lms.comments().size()), Result.LMS_ONLY,
        lms.pearsActivityId() == null ? "Added in LMS" : "From PEARS activity %s".formatted(lms.pearsActivityId()));
  }

  private static String category(LmsWorkProgramme.Activity lms) {
    return lms.otherCategoryName() == null
        ? lms.category().getDisplayName()
        : "%s: %s".formatted(lms.category().getDisplayName(), lms.otherCategoryName());
  }

  @Nullable
  private static String lastStatus(LmsWorkProgramme.Activity lms) {
    return lms.statuses().isEmpty() ? null : lms.statuses().getLast().status().getDisplayName();
  }

  private static String date(LmsWorkProgramme.Activity lms) {
    return switch (lms.dateOption()) {
      case WITHIN_A_PHASE -> "Within " + (lms.phaseType() == null ? "?" : lms.phaseType().getDisplayName());
      case RELATIVE_DATE -> "%s + %s, due %s".formatted(
          lms.termType() == null ? "?" : lms.termType().getDisplayName(),
          duration(lms.relativeDuration()),
          Objects.toString(lms.dueDate(), "?"));
      default -> "Within " + (lms.termType() == null ? "?" : lms.termType().getDisplayName());
    };
  }

  private static String duration(@Nullable ThreeFieldDuration duration) {
    return duration == null
        ? "?"
        : "%dy %dm %dd".formatted(duration.years(), duration.months(), duration.days());
  }

  /**
   * Each comment the migration writes with an LMS comment of the same text, then the migration's comments LMS has
   * beyond those, then the ones added in LMS.
   */
  private static List<CommentRow> scheduleComments(
      List<MappedActivityHistory.Comment> expected,
      List<LmsWorkProgramme.ScheduleComment> lms
  ) {
    var migrated = new ArrayList<>(lms.stream()
        .filter(LmsWorkProgramme.ScheduleComment::migrated)
        .map(LmsWorkProgramme.ScheduleComment::text)
        .toList());

    var rows = new ArrayList<CommentRow>();
    for (var comment : expected) {
      rows.add(migrated.remove(comment.text())
          ? new CommentRow(comment.text(), comment.text(), Result.MATCH)
          : new CommentRow(comment.text(), null, Result.MISSING_IN_LMS));
    }
    migrated.forEach(text -> rows.add(new CommentRow(null, text, Result.LMS_ONLY)));
    rows.addAll(addedInLms(lms));
    return rows;
  }

  private static List<CommentRow> lmsOnlyComments(LmsWorkProgramme lms) {
    var rows = new ArrayList<CommentRow>();
    lms.scheduleComments().stream()
        .filter(LmsWorkProgramme.ScheduleComment::migrated)
        .forEach(comment -> rows.add(new CommentRow(null, comment.text(), Result.LMS_ONLY)));
    rows.addAll(addedInLms(lms.scheduleComments()));
    return rows;
  }

  private static List<CommentRow> addedInLms(List<LmsWorkProgramme.ScheduleComment> lms) {
    return lms.stream()
        .filter(comment -> !comment.migrated())
        .map(comment -> new CommentRow(null, comment.text(), Result.ADDED_IN_LMS))
        .toList();
  }

  private static String key(@Nullable Long pedWpId, @Nullable String pearsActivityId) {
    return pedWpId + ":" + pearsActivityId;
  }
}
