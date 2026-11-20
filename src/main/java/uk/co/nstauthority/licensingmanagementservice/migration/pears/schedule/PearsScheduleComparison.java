package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

/**
 * A licence's PEARS schedule side by side with its LMS schedule, event by event (plan, design section 6).
 *
 * @param problem          why there is nothing to compare, for example the licence isn't in LMS; otherwise null
 * @param migrationFailure why the migration doesn't migrate the licence, when it fails or skips it. Its PEARS events
 *                         then show as not migrated
 * @param versions         one table per schedule version: the current one, or every one oldest first
 */
public record PearsScheduleComparison(
    String licenceReference,
    @Nullable String problem,
    @Nullable String migrationFailure,
    List<Version> versions
) {

  public PearsScheduleComparison {
    versions = List.copyOf(versions);
  }

  static PearsScheduleComparison problem(String licenceReference, String problem) {
    return new PearsScheduleComparison(licenceReference, problem, null, List.of());
  }

  /**
   * Whether the licence needs looking into: it couldn't be compared, the migration fails it, or a row shows an issue.
   */
  public boolean hasIssues() {
    return problem != null
        || migrationFailure != null
        || versions.stream().flatMap(version -> version.rows().stream()).anyMatch(row -> row.result().isIssue());
  }

  public boolean hasEventsNotMigrated() {
    return versions.stream().flatMap(version -> version.rows().stream())
        .anyMatch(row -> row.result() == Result.NOT_MIGRATED);
  }

  public Map<Result, Integer> resultCounts() {
    var counts = new EnumMap<Result, Integer>(Result.class);
    versions.forEach(version -> version.rows().forEach(row -> counts.merge(row.result(), 1, Integer::sum)));
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
    versions.forEach(version -> version.rows().stream()
        .filter(row -> row.result().isIssue())
        .forEach(row -> lines.add(describe(version, row))));
    return lines;
  }

  /**
   * One row on a line, for example {@code version 2, Rate In Second Term: PEARS 2018-12-01 150, LMS 2018-12-01 175: Value
   * differs}.
   */
  static String describe(Version version, Row row) {
    // A relinquishment's caption is its event's name again
    var what = row.caption().isBlank() || row.caption().equals(row.event())
        ? row.event()
        : "%s %s".formatted(row.event(), row.caption());
    return "version %s, %s: PEARS %s %s, LMS %s %s: %s%s".formatted(
        version.pearsPlsdId() == null ? "(LMS only)" : version.pearsPlsdId(),
        what,
        Objects.toString(row.pearsDate(), ""),
        Objects.toString(row.pearsValue(), ""),
        Objects.toString(row.lmsDate(), ""),
        Objects.toString(row.lmsValue(), ""),
        row.resultText(),
        row.detail() == null ? "" : " (%s)".formatted(row.detail())
    ).replaceAll(" {2,}", " ").replace(" ,", ",").replace(" :", ":");
  }

  /**
   * A PEARS version and the LMS schedule detail migrated from it. Either side is missing when it has no counterpart.
   *
   * @param note what else is known about the pairing, for example why the migration left the PEARS version out
   */
  public record Version(
      @Nullable Long pearsPlsdId,
      @Nullable LocalDateTime pearsSaved,
      @Nullable LocalDate pearsStartDate,
      @Nullable UUID lmsDetailId,
      @Nullable LicenceScheduleDetailStatus lmsStatus,
      @Nullable LocalDate lmsStartDate,
      @Nullable String note,
      List<Row> rows
  ) {

    public Version {
      rows = List.copyOf(rows);
    }
  }

  /**
   * One PEARS event and what LMS holds for it, or an LMS event with no PEARS event.
   *
   * @param pearsIndex   the event's position in the PEARS version's {@code EVENT_LIST}
   * @param pearsOffset  the offset PEARS stores, from the schedule start date
   * @param lmsBasis     what LMS dates the event from: the anchor and offset it stores
   * @param detail       anything else needed to read the row, for example why an event was not migrated
   * @param comparedWith the date the LMS date was compared with: PEARS's, or the date expected where PEARS's isn't used
   */
  public record Row(
      String event,
      String caption,
      @Nullable Integer pearsIndex,
      @Nullable String pearsOffset,
      @Nullable LocalDate pearsDate,
      @Nullable String pearsValue,
      @Nullable String lmsBasis,
      @Nullable LocalDate lmsDate,
      @Nullable String lmsValue,
      Result result,
      @Nullable String detail,
      @Nullable LocalDate comparedWith
  ) {

    /**
     * A row whose LMS date was compared with PEARS's.
     */
    public Row(
        String event,
        String caption,
        @Nullable Integer pearsIndex,
        @Nullable String pearsOffset,
        @Nullable LocalDate pearsDate,
        @Nullable String pearsValue,
        @Nullable String lmsBasis,
        @Nullable LocalDate lmsDate,
        @Nullable String lmsValue,
        Result result,
        @Nullable String detail
    ) {
      this(event, caption, pearsIndex, pearsOffset, pearsDate, pearsValue, lmsBasis, lmsDate, lmsValue, result, detail,
          pearsDate);
    }

    /**
     * The result, with how far apart the dates are when they differ: from PEARS's date, or from the date expected where
     * PEARS's isn't used, as in a version dated from its offsets.
     */
    public String resultText() {
      if (result == Result.DIFFERS && comparedWith != null && lmsDate != null) {
        var days = ChronoUnit.DAYS.between(comparedWith, lmsDate);
        return comparedWith.equals(pearsDate)
            ? "Differs by %+d days".formatted(days)
            : "Differs by %+d days from the date expected".formatted(days);
      }
      return result.getText();
    }

    /**
     * The date the row is ordered by: PEARS's, or LMS's where PEARS has none to go by, as for an event dated from its
     * offset.
     */
    @Nullable
    LocalDate sortingDate() {
      if (pearsDate == null || result == Result.FROM_OFFSET) {
        return lmsDate;
      }
      return comparedWith == null ? pearsDate : comparedWith;
    }
  }

  /**
   * What a row shows. An issue is a difference to look into: the rest are matches, or differences the migration makes on
   * purpose.
   */
  public enum Result {
    MATCH("Match", false),
    DIFFERS("Differs", true),
    VALUE_DIFFERS("Value differs", true),
    CARRIED_ON("Carried on", false),
    FROM_OFFSET("Dated from its offset", false),
    NO_START_DATE("No start date in PEARS", false),
    NO_EXPIRY("No expiry in PEARS", false),
    ORIGINAL_EVENT_DIFFERS("Original event differs", true),
    MISSING_IN_LMS("Missing in LMS", true),
    NOT_MIGRATED("Not migrated", false),
    LMS_ONLY("LMS only", true);

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
