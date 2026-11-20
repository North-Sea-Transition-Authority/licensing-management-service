package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Row;

class PearsScheduleComparisonReportTest {

  private static final LocalDate START = LocalDate.of(2014, Month.DECEMBER, 1);
  private static final UUID DETAIL_ID = UUID.fromString("7d2c3c3e-0f7a-4d0e-9a51-2a6c1f1d2b11");

  @Test
  void html_showsEachLicenceVersionAndRowWithItsCounts() {
    var html = PearsScheduleComparisonReport.html(List.of(comparison(
        row("Term", Result.MATCH, null),
        row("Other activity", Result.DIFFERS, null)
    )));

    assertThat(html)
        .contains("<h2>P2244</h2>")
        .contains("PEARS version 2, saved 2019-01-01T09:00, starting 2014-12-01 | LMS ACTIVE detail " + DETAIL_ID)
        .contains("<p>Match: 1, Differs: 1</p>")
        .contains("<tr class=\"check\"><td>4</td><td>Other activity</td>")
        .contains("<td>Differs by +3 days</td>");
  }

  @Test
  void html_escapesWhatPearsHolds() {
    var html = PearsScheduleComparisonReport.html(List.of(comparison(
        row("Area event", Result.MATCH, "<script>alert(1)</script> & co")
    )));

    assertThat(html)
        .doesNotContain("<script>")
        .contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; co");
  }

  @Test
  void html_whenThereIsNothingToCompare_saysWhy() {
    var html = PearsScheduleComparisonReport.html(List.of(PearsScheduleComparison.problem("P9999", "Not a licence in LMS")));

    assertThat(html)
        .contains("<p>Nothing to compare</p>")
        .contains("<h2>P9999</h2><p class=\"note\">Not a licence in LMS</p>");
  }

  @Test
  void csv_writesARowPerComparisonRowWithItsLicenceAndVersion() {
    var csv = PearsScheduleComparisonReport.csv(List.of(
        comparison(row("Other activity", Result.DIFFERS, "Drill, or drop \"now\"")),
        PearsScheduleComparison.problem("P9999", "Not a licence in LMS")
    ));

    assertThat(csv.split("\r\n")).containsExactly(
        "Licence,PEARS version,LMS detail,LMS status,Note,PEARS #,Event,Caption,PEARS offset,PEARS date,PEARS value,"
            + "LMS basis,LMS date,LMS value,Result,Detail",
        "P2244,2," + DETAIL_ID + ",ACTIVE,,4,Other activity,Caption,4y 0m 0d,2018-12-01,150 YEARLY,Initial Term start,"
            + "2018-12-04,150,Differs by +3 days,\"Drill, or drop \"\"now\"\"\"",
        "P9999,,,,Not a licence in LMS,,,,,,,,,,,"
    );
  }

  @Test
  void csvCell_keepsAValueFromBeingReadAsAFormula() {
    assertThat(PearsScheduleComparisonReport.csvCell("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
    assertThat(PearsScheduleComparisonReport.csvCell("@SUM(A1)")).isEqualTo("'@SUM(A1)");
    assertThat(PearsScheduleComparisonReport.csvCell("50% Relinquishment")).isEqualTo("50% Relinquishment");
  }

  private static PearsScheduleComparison comparison(Row... rows) {
    return new PearsScheduleComparison("P2244", null, null, List.of(new PearsScheduleComparison.Version(
        2L, LocalDateTime.of(2019, Month.JANUARY, 1, 9, 0), START, DETAIL_ID, LicenceScheduleDetailStatus.ACTIVE, START,
        null, List.of(rows))));
  }

  private static Row row(String event, Result result, String detail) {
    return new Row(event, "Caption", 4, "4y 0m 0d", LocalDate.of(2018, Month.DECEMBER, 1), "150 YEARLY",
        "Initial Term start", LocalDate.of(2018, Month.DECEMBER, 4), "150", result, detail);
  }
}
