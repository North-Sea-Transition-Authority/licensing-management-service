package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.web.util.HtmlUtils;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparisonReport;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.CommentRow;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Row;

/**
 * Writes work programme comparisons out as an HTML page or a CSV file. Every value is escaped: PEARS descriptions and
 * notes are free text.
 */
final class PearsWorkProgrammeComparisonReport {

  static final List<String> COLUMNS = List.of(
      "PEARS activity", "PEARS type", "PEARS title", "PEARS description", "PEARS status", "LMS category",
      "LMS commitment", "LMS description", "LMS date", "LMS status", "LMS comments", "Result", "Detail"
  );

  static final List<String> COMMENT_COLUMNS = List.of("PEARS note", "LMS schedule comment", "Result");

  private PearsWorkProgrammeComparisonReport() {
  }

  /**
   * The page.
   *
   * @param scope what was compared, for example how many licences had issues when only those are shown
   */
  static String html(List<PearsWorkProgrammeComparison> comparisons, String scope) {
    var html = new StringBuilder()
        .append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
        .append("<title>PEARS work programme comparison</title><style>")
        .append(PearsScheduleComparisonReport.STYLE)
        .append("</style></head><body>")
        .append("<h1>PEARS work programme comparison</h1>")
        .append("<p>").append(escape(scope)).append("</p>")
        .append("<p>").append(escape(countsText(totalCounts(comparisons)))).append("</p>");

    for (var comparison : comparisons) {
      html.append("<h2>").append(escape(comparison.licenceReference())).append("</h2>");
      if (comparison.problem() != null) {
        html.append("<p class=\"note\">").append(escape(comparison.problem())).append("</p>");
        continue;
      }
      if (comparison.migrationFailure() != null) {
        html.append("<p class=\"note\">The migration doesn't migrate this licence: ")
            .append(escape(comparison.migrationFailure())).append("</p>");
      }
      html.append("<p>").append(escape(countsText(comparison.resultCounts()))).append("</p>");
      appendTable(html, COLUMNS, comparison.rows().stream().map(row -> new Line(cells(row), row.result())).toList());
      if (!comparison.scheduleComments().isEmpty()) {
        html.append("<h3>Schedule comments</h3>");
        appendTable(html, COMMENT_COLUMNS, comparison.scheduleComments().stream()
            .map(row -> new Line(List.of(text(row.pears()), text(row.lms()), row.result().getText()), row.result()))
            .toList());
      }
    }
    return html.append("</body></html>").toString();
  }

  /**
   * One table: a line per activity, then a line per schedule comment, with the note and the comment in the description
   * columns.
   */
  static String csv(List<PearsWorkProgrammeComparison> comparisons) {
    var csv = new StringBuilder();
    appendCsvLine(csv, Stream.concat(Stream.of("Licence", "Kind", "Note"), COLUMNS.stream()).toList());

    for (var comparison : comparisons) {
      if (comparison.problem() != null) {
        appendCsvLine(csv, Stream.concat(Stream.of(comparison.licenceReference(), "", comparison.problem()),
            COLUMNS.stream().map(column -> "")).toList());
        continue;
      }
      var note = comparison.migrationFailure() == null
          ? ""
          : "The migration doesn't migrate this licence: " + comparison.migrationFailure();
      for (var row : comparison.rows()) {
        appendCsvLine(csv, Stream.concat(Stream.of(comparison.licenceReference(), "Activity", note),
            cells(row).stream()).toList());
      }
      for (var row : comparison.scheduleComments()) {
        appendCsvLine(csv, Stream.concat(Stream.of(comparison.licenceReference(), "Schedule comment", note),
            commentCells(row).stream()).toList());
      }
    }
    return csv.toString();
  }

  private static void appendTable(StringBuilder html, List<String> columns, List<Line> lines) {
    html.append("<table><thead><tr>");
    columns.forEach(column -> html.append("<th>").append(escape(column)).append("</th>"));
    html.append("</tr></thead><tbody>");
    for (var line : lines) {
      html.append("<tr").append(rowClass(line.result())).append('>');
      line.cells().forEach(cell -> html.append("<td>").append(escape(cell)).append("</td>"));
      html.append("</tr>");
    }
    html.append("</tbody></table>");
  }

  private static String rowClass(Result result) {
    if (result.isIssue()) {
      return " class=\"check\"";
    }
    return result == Result.NOT_MIGRATED ? " class=\"not-migrated\"" : "";
  }

  private static List<String> cells(Row row) {
    return List.of(
        text(row.pearsActivityId()), text(row.pearsType()), text(row.pearsTitle()), text(row.pearsDescription()),
        text(row.pearsStatus()), text(row.lmsCategory()), text(row.lmsCommitment()), text(row.lmsDescription()),
        text(row.lmsDate()), text(row.lmsStatus()), text(row.comments()), row.result().getText(), text(row.detail())
    );
  }

  private static List<String> commentCells(CommentRow row) {
    return List.of("", "", "", text(row.pears()), "", "", "", text(row.lms()), "", "", "", row.result().getText(), "");
  }

  private static Map<Result, Integer> totalCounts(List<PearsWorkProgrammeComparison> comparisons) {
    var counts = new EnumMap<Result, Integer>(Result.class);
    comparisons.forEach(comparison -> comparison.resultCounts().forEach((result, count) ->
        counts.merge(result, count, Integer::sum)));
    return counts;
  }

  static String countsText(Map<Result, Integer> counts) {
    if (counts.isEmpty()) {
      return "Nothing to compare";
    }
    return counts.entrySet().stream()
        .map(entry -> "%s: %d".formatted(entry.getKey().getText(), entry.getValue()))
        .collect(Collectors.joining(", "));
  }

  private static void appendCsvLine(StringBuilder csv, List<String> cells) {
    csv.append(cells.stream().map(PearsScheduleComparisonReport::csvCell).collect(Collectors.joining(",")))
        .append("\r\n");
  }

  private static String escape(String value) {
    return HtmlUtils.htmlEscape(value);
  }

  private static String text(@Nullable Object value) {
    return value == null ? "" : value.toString();
  }

  private record Line(List<String> cells, Result result) {
  }
}
