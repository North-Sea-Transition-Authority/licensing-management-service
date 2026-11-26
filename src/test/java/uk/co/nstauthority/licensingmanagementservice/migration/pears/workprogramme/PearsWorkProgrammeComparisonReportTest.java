package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.CommentRow;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Row;

class PearsWorkProgrammeComparisonReportTest {

  private static final PearsWorkProgrammeComparison COMPARISON = new PearsWorkProgrammeComparison(
      "P2244",
      null,
      null,
      List.of(
          new Row("701", "OTHER", null, "<b>Studies</b>, phase 1", "NOT_STARTED", "Other activity", "Conditional",
              "<b>Studies</b>, phase 1", "Within Initial Term", "Open", "0", Result.MAPPED, null),
          new Row("702", "OTHER", null, "None", "NOT_STARTED", null, null, null, null, null, null,
              Result.NOT_MIGRATED, "No activity")
      ),
      List.of(new CommentRow("=SUM(A1)", "=SUM(A1)", Result.MATCH))
  );

  @Test
  void html_escapesEveryValueAndShadesTheRowsNotMigrated() {
    var html = PearsWorkProgrammeComparisonReport.html(List.of(COMPARISON, PearsWorkProgrammeComparison.problem(
        "P1", "Not a licence in LMS")), "2 licences compared");

    assertThat(html)
        .contains("&lt;b&gt;Studies&lt;/b&gt;")
        .doesNotContain("<b>Studies")
        .contains("<tr class=\"not-migrated\">")
        .contains("<h3>Schedule comments</h3>")
        .contains("Match: 1, Mapped: 1, Not migrated: 1")
        .contains("<p class=\"note\">Not a licence in LMS</p>");
  }

  @Test
  void csv_isOneTableWithALinePerActivityAndScheduleComment() {
    var lines = PearsWorkProgrammeComparisonReport.csv(List.of(COMPARISON)).split("\r\n");

    assertThat(lines).hasSize(4);
    assertThat(lines[0]).startsWith("Licence,Kind,Note,PEARS activity,");
    assertThat(lines[1]).startsWith("P2244,Activity,,701,OTHER,,\"<b>Studies</b>, phase 1\",NOT_STARTED,");
    assertThat(lines[3]).startsWith("P2244,Schedule comment,,,,,'=SUM(A1),");
  }
}
