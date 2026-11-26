package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.CommentRow;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Row;

class PearsWorkProgrammeComparerTest {

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);
  private static final LocalDateTime SAVED = LocalDateTime.of(2015, Month.MARCH, 2, 10, 0);
  private static final LocalDate START = LocalDate.of(2014, Month.DECEMBER, 1);
  private static final MigratedSchedule SCHEDULE = new MigratedSchedule(START,
      List.of(new MigratedSchedule.Term(TermType.INITIAL, START, LocalDate.of(2018, Month.NOVEMBER, 30))),
      List.of(), List.of());

  @Test
  void compare_whenTheMigrationFailsTheLicence_showsItsActivitiesNotMigratedAndAnyInLmsAsLmsOnly() {
    var programme = programme(activity("701", "IN_PROGRESS"));
    var lms = new LmsWorkProgramme(
        List.of(lmsActivity(11L, "701", "Studies", WorkProgrammeStatus.IN_PROGRESS)),
        List.of(new LmsWorkProgramme.ScheduleComment("A general note (PEARS note)", true)));

    var comparison = PearsWorkProgrammeComparer.compare("P2244", List.of(programme), null, "Two INITIAL", lms);

    assertThat(comparison.migrationFailure()).isEqualTo("Two INITIAL");
    assertThat(comparison.rows()).extracting(Row::pearsActivityId, Row::result, Row::detail).containsExactly(
        tuple("701", Result.DIFFERS,
            "In LMS, but the migration wouldn't migrate it: " + PearsWorkProgrammeComparer.NOT_MAPPED));
    assertThat(comparison.scheduleComments()).extracting(CommentRow::result).containsExactly(Result.LMS_ONLY);
    assertThat(comparison.hasIssues()).isTrue();
  }

  @Test
  void compare_whenLmsHasAnActivityNoPearsActivityBecame_showsItLmsOnly() {
    var programme = programme(activity("701", "IN_PROGRESS"));
    var mapped = PearsWorkProgrammeMapper.map(P2244, List.of(programme), SCHEDULE);
    var lms = new LmsWorkProgramme(List.of(lmsActivity(null, null, "Added", WorkProgrammeStatus.OPEN)), List.of());

    var comparison = PearsWorkProgrammeComparer.compare("P2244", List.of(programme), mapped, null, lms);

    assertThat(comparison.rows()).extracting(Row::pearsActivityId, Row::result, Row::detail).containsExactly(
        tuple("701", Result.MISSING_IN_LMS, null),
        tuple(null, Result.LMS_ONLY, "Added in LMS")
    );
    assertThat(comparison.issueDescriptions()).containsExactly(
        "activity 701: Missing in LMS",
        "activity (LMS only): LMS only (Added in LMS)"
    );
  }

  @Test
  void compare_whenTheStatusHistoryDiffers_saysSo() {
    var programme = programme(activity("701", "IN_PROGRESS"));
    var mapped = PearsWorkProgrammeMapper.map(P2244, List.of(programme), SCHEDULE);
    var expected = mapped.activities().getFirst();
    var lmsActivity = new LmsWorkProgramme.Activity(UUID.randomUUID(), 11L, "701", expected.activity().category(),
        expected.activity().otherCategoryName(), expected.activity().commitment(), expected.activity().description(),
        expected.date().dateOption(), expected.date().termType(), null, null, null,
        List.of(new LmsWorkProgramme.Status(WorkProgrammeStatus.COMPLETE, PearsWorkProgrammeWriter.instant(SAVED))),
        expected.history().comments().stream().map(MappedActivityHistory.Comment::text).toList());

    var comparison = PearsWorkProgrammeComparer.compare(
        "P2244", List.of(programme), mapped, null, new LmsWorkProgramme(List.of(lmsActivity), List.of()));

    assertThat(comparison.rows()).extracting(Row::result, Row::detail)
        .containsExactly(tuple(Result.DIFFERS, "Status history differs"));
  }

  @Test
  void compare_whenTheMigrationWritesACommentLmsDoesNotHave_showsItMissing() {
    var programme = new PearsWorkProgramme(P2244, 11L, 110L, SAVED, "INITIAL", null, "ACTIVE", null, List.of(),
        List.of(new PearsNote(1, "GENERAL", null, null, null, SAVED, "Jane Smith", "General note")));
    var mapped = PearsWorkProgrammeMapper.map(P2244, List.of(programme), SCHEDULE);

    var comparison = PearsWorkProgrammeComparer.compare("P2244", List.of(programme), mapped, null,
        LmsWorkProgramme.none());

    assertThat(comparison.scheduleComments()).extracting(CommentRow::pears, CommentRow::result)
        .containsExactly(tuple("General note (PEARS note by Jane Smith)", Result.MISSING_IN_LMS));
    assertThat(comparison.issueDescriptions())
        .containsExactly("schedule comment: Missing in LMS: General note (PEARS note by Jane Smith)");
  }

  @Test
  void compare_whenLmsReturnsTheStatusesAndCommentsInAnotherOrder_findsNoDifference() {
    var programme = new PearsWorkProgramme(P2244, 11L, 110L, SAVED, "INITIAL", null, "ACTIVE", null,
        List.of(activity("701", "COMPLETE")),
        List.of(
            new PearsNote(1, "NOTE", "701", null, "IN_PROGRESS", SAVED.plusDays(1), "Jane Smith", "Started"),
            new PearsNote(2, "NOTE", "701", null, "COMPLETE", SAVED.plusDays(1), "Jane Smith", "Done")
        ));
    var mapped = PearsWorkProgrammeMapper.map(P2244, List.of(programme), SCHEDULE);
    var expected = mapped.activities().getFirst();
    var statuses = new ArrayList<>(PearsWorkProgrammeComparer.lmsStatuses(expected.history()));
    var comments = new ArrayList<>(expected.history().comments().stream()
        .map(MappedActivityHistory.Comment::text)
        .toList());
    Collections.reverse(statuses);
    Collections.reverse(comments);
    var lmsActivity = new LmsWorkProgramme.Activity(UUID.randomUUID(), 11L, "701", expected.activity().category(),
        expected.activity().otherCategoryName(), expected.activity().commitment(), expected.activity().description(),
        expected.date().dateOption(), expected.date().termType(), null, null, null, statuses, comments);

    var comparison = PearsWorkProgrammeComparer.compare(
        "P2244", List.of(programme), mapped, null, new LmsWorkProgramme(List.of(lmsActivity), List.of()));

    assertThat(statuses).hasSizeGreaterThan(1);
    assertThat(comparison.rows()).extracting(Row::result).containsExactly(Result.MATCH);
  }

  private static PearsWorkProgramme programme(PearsActivity... activities) {
    return new PearsWorkProgramme(P2244, 11L, 110L, SAVED, "INITIAL", null, "ACTIVE", null, List.of(activities),
        List.of());
  }

  private static PearsActivity activity(String activityId, String status) {
    return new PearsActivity(1, PearsActivity.ListKind.FLAT, null, activityId, "OTHER", null, status, null, "Studies",
        SAVED);
  }

  private static LmsWorkProgramme.Activity lmsActivity(
      Long pedWpId,
      String pearsActivityId,
      String description,
      WorkProgrammeStatus status
  ) {
    return new LmsWorkProgramme.Activity(UUID.randomUUID(), pedWpId, pearsActivityId,
        WorkProgrammeActivityCategory.OTHER_ACTIVITY, "Other activity", WorkProgrammeActivityCommitment.CONDITIONAL,
        description, WorkProgrammeActivityDateOption.WITHIN_A_TERM, TermType.INITIAL, null, null, null,
        List.of(new LmsWorkProgramme.Status(status, PearsWorkProgrammeWriter.instant(SAVED))), List.of());
  }
}
