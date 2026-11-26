package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.web.util.HtmlUtils;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Row;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Version;

/**
 * Writes comparisons out as an HTML page or a CSV file. Every value is escaped: PEARS comments are free text.
 */
public final class PearsScheduleComparisonReport {

  static final List<String> COLUMNS = List.of(
      "PEARS #", "Event", "Caption", "PEARS offset", "PEARS date", "PEARS value", "LMS basis", "LMS date", "LMS value",
      "Result", "Detail"
  );

  public static final String STYLE = """
      body { font-family: Arial, sans-serif; font-size: 14px; margin: 16px; }
      table { border-collapse: collapse; margin-bottom: 24px; }
      th, td { border: 1px solid #b1b4b6; padding: 4px 6px; text-align: left; vertical-align: top; }
      th { background: #f3f2f1; }
      tr.check td { background: #fbe4e4; }
      tr.not-migrated td { background: #fff7bf; }
      .note { font-style: italic; }""";

  private PearsScheduleComparisonReport() {
  }

  static String html(List<PearsScheduleComparison> comparisons) {
    return html(comparisons, null);
  }

  /**
   * The page.
   *
   * @param scope what was compared, when it isn't simply the licences shown, for example how many had issues
   */
  static String html(List<PearsScheduleComparison> comparisons, @Nullable String scope) {
    var html = new StringBuilder()
        .append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
        .append("<title>PEARS schedule comparison</title><style>").append(STYLE).append("</style></head><body>")
        .append("<h1>PEARS schedule comparison</h1>");
    if (scope != null) {
      html.append("<p>").append(escape(scope)).append("</p>");
    }
    html.append("<p>").append(escape(countsText(totalCounts(comparisons)))).append("</p>");

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
      comparison.versions().forEach(version -> appendVersion(html, version));
    }
    return html.append("</body></html>").toString();
  }

  static String csv(List<PearsScheduleComparison> comparisons) {
    var csv = new StringBuilder();
    appendCsvLine(csv, Stream.concat(
        Stream.of("Licence", "PEARS version", "LMS detail", "LMS status", "Note"), COLUMNS.stream()).toList());

    for (var comparison : comparisons) {
      if (comparison.problem() != null) {
        // As wide as every other line, so the file reads as one table
        appendCsvLine(csv, Stream.concat(
            Stream.of(comparison.licenceReference(), "", "", "", comparison.problem()),
            COLUMNS.stream().map(column -> "")).toList());
        continue;
      }
      for (var version : comparison.versions()) {
        var notMigrated = comparison.migrationFailure() == null
            ? ""
            : "The migration doesn't migrate this licence: " + comparison.migrationFailure();
        var note = Objects.requireNonNullElse(version.note(), notMigrated);
        for (var row : version.rows()) {
          appendCsvLine(csv, Stream.concat(
              Stream.of(comparison.licenceReference(), text(version.pearsPlsdId()), text(version.lmsDetailId()),
                  text(version.lmsStatus()), note),
              cells(row).stream()).toList());
        }
      }
    }
    return csv.toString();
  }

  private static void appendVersion(StringBuilder html, Version version) {
    html.append("<h3>").append(escape(versionHeading(version))).append("</h3>");
    if (version.note() != null) {
      html.append("<p class=\"note\">").append(escape(version.note())).append("</p>");
    }
    html.append("<table><thead><tr>");
    COLUMNS.forEach(column -> html.append("<th>").append(escape(column)).append("</th>"));
    html.append("</tr></thead><tbody>");
    for (var row : version.rows()) {
      html.append("<tr").append(rowClass(row.result())).append('>');
      cells(row).forEach(cell -> html.append("<td>").append(escape(cell)).append("</td>"));
      html.append("</tr>");
    }
    html.append("</tbody></table>");
  }

  private static String versionHeading(Version version) {
    var pearsStart = version.pearsStartDate() == null ? "(no start date)" : version.pearsStartDate().toString();
    var pears = version.pearsPlsdId() == null
        ? "No PEARS version"
        : "PEARS version %d, saved %s, starting %s".formatted(version.pearsPlsdId(), text(version.pearsSaved()),
            pearsStart);
    var lms = version.lmsDetailId() == null
        ? "no LMS detail"
        : "LMS %s detail %s, starting %s".formatted(version.lmsStatus(), version.lmsDetailId(),
            text(version.lmsStartDate()));
    return pears + " | " + lms;
  }

  private static String rowClass(Result result) {
    if (result.isIssue()) {
      return " class=\"check\"";
    }
    return result == Result.NOT_MIGRATED ? " class=\"not-migrated\"" : "";
  }

  private static List<String> cells(Row row) {
    return List.of(
        text(row.pearsIndex()), row.event(), row.caption(), text(row.pearsOffset()), text(row.pearsDate()),
        text(row.pearsValue()), text(row.lmsBasis()), text(row.lmsDate()), text(row.lmsValue()), row.resultText(),
        text(row.detail())
    );
  }

  private static Map<Result, Integer> totalCounts(List<PearsScheduleComparison> comparisons) {
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

  /**
   * Quoted when it holds a comma, quote or line break, and kept from being read as a formula by a spreadsheet.
   */
  public static String csvCell(String value) {
    var cell = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
    if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
      return "\"" + cell.replace("\"", "\"\"") + "\"";
    }
    return cell;
  }

  private static String escape(String value) {
    return HtmlUtils.htmlEscape(value);
  }

  private static String text(@Nullable Object value) {
    return value == null ? "" : value.toString();
  }
}
