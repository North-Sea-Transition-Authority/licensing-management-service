package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A licence's current PEARS work programmes side by side with its LMS work programme, activity by activity, and its
 * general notes against its schedule comments (plan, design section 6).
 *
 * @param problem          why there is nothing to compare, for example the licence isn't in LMS; otherwise null
 * @param migrationFailure why the migration doesn't migrate the licence, when it fails or skips it. Its PEARS
 *                         activities then show as not migrated
 * @param rows             one per PEARS activity, in PEARS order, then one per LMS activity no PEARS activity became
 * @param scheduleComments the comments the migration writes to the schedule against those LMS holds
 */
public record PearsWorkProgrammeComparison(
    String licenceReference,
    @Nullable String problem,
    @Nullable String migrationFailure,
    List<Row> rows,
    List<CommentRow> scheduleComments
) {

  public PearsWorkProgrammeComparison {
    rows = List.copyOf(rows);
    scheduleComments = List.copyOf(scheduleComments);
  }

  static PearsWorkProgrammeComparison problem(String licenceReference, String problem) {
    return new PearsWorkProgrammeComparison(licenceReference, problem, null, List.of(), List.of());
  }

  /**
   * Whether the licence needs looking into: it couldn't be compared, the migration fails it, or a row shows an issue.
   */
  public boolean hasIssues() {
    return problem != null || migrationFailure != null || results().anyMatch(Result::isIssue);
  }

  public boolean hasActivitiesNotMigrated() {
    return rows.stream().anyMatch(row -> row.result() == Result.NOT_MIGRATED);
  }

  public Map<Result, Integer> resultCounts() {
    var counts = new EnumMap<Result, Integer>(Result.class);
    results().forEach(result -> counts.merge(result, 1, Integer::sum));
    return counts;
  }

  /**
   * What needs looking into, a line each: why the licence couldn't be compared or the migration fails it, then each row
   * with an issue.
   */
  public List<String> issueDescriptions() {
    var lines = new ArrayList<String>();
    if (problem != null) {
      lines.add(problem);
    }
    if (migrationFailure != null) {
      lines.add("The migration doesn't migrate this licence: " + migrationFailure);
    }
    rows.stream()
        .filter(row -> row.result().isIssue())
        .forEach(row -> lines.add("activity %s: %s%s".formatted(
            Objects.requireNonNullElse(row.pearsActivityId(), "(LMS only)"),
            row.result().getText(),
            row.detail() == null ? "" : " (%s)".formatted(row.detail()))));
    scheduleComments.stream()
        .filter(row -> row.result().isIssue())
        .forEach(row -> lines.add("schedule comment: %s: %s".formatted(
            row.result().getText(), Objects.requireNonNullElse(row.pears(), row.lms()))));
    return lines;
  }

  private Stream<Result> results() {
    return Stream.concat(rows.stream().map(Row::result), scheduleComments.stream().map(CommentRow::result));
  }

  /**
   * One PEARS activity and the LMS activity it became, or an LMS activity no PEARS activity became.
   *
   * @param pearsActivityId the activity's {@code ACTIVITY_ID}; null for an LMS activity with none
   * @param lmsDate         where LMS places the activity, for example {@code Within Initial Term}
   * @param comments        how many comments the LMS activity has
   * @param detail          anything else needed to read the row: why it wasn't migrated, or what differs
   */
  public record Row(
      @Nullable String pearsActivityId,
      @Nullable String pearsType,
      @Nullable String pearsTitle,
      @Nullable String pearsDescription,
      @Nullable String pearsStatus,
      @Nullable String lmsCategory,
      @Nullable String lmsCommitment,
      @Nullable String lmsDescription,
      @Nullable String lmsDate,
      @Nullable String lmsStatus,
      @Nullable String comments,
      Result result,
      @Nullable String detail
  ) {
  }

  /**
   * A schedule comment the migration writes, and the LMS schedule comment it became.
   */
  public record CommentRow(@Nullable String pears, @Nullable String lms, Result result) {
  }

  /**
   * What a row shows. An issue is a difference to look into: the rest are matches, or differences the migration makes on
   * purpose.
   */
  public enum Result {
    MATCH("Match", false),
    /**
     * As the migration maps it, through a mapping that changes the PEARS value, for example {@code NOT_STARTED} to Open.
     */
    MAPPED("Mapped", false),
    DIFFERS("Differs", true),
    NOT_MIGRATED("Not migrated", false),
    MISSING_IN_LMS("Missing in LMS", true),
    LMS_ONLY("LMS only", true),
    /**
     * A schedule comment someone added in LMS, which isn't the migration's.
     */
    ADDED_IN_LMS("Added in LMS", false);

    private final String text;
    private final boolean issue;

    Result(String text, boolean issue) {
      this.text = text;
      this.issue = issue;
    }

    public String getText() {
      return text;
    }

    public boolean isIssue() {
      return issue;
    }
  }
}
