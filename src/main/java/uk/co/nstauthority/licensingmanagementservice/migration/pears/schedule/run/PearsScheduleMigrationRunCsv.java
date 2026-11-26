package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparisonReport;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison;

/**
 * A run's licences as a CSV file, a row each: what became of it, and for a dry run its issues and how many comparison
 * rows had each result. The mapper's notes go in the last column.
 */
public final class PearsScheduleMigrationRunCsv {

  private static final String LINE_END = "\r\n";

  private PearsScheduleMigrationRunCsv() {
  }

  public static String csv(List<PearsScheduleMigrationRunService.RunLicence> licences) {
    // Only the results some licence of the run had, in the order the comparison reports list them
    var scheduleResults = Arrays.stream(PearsScheduleComparison.Result.values())
        .map(PearsScheduleComparison.Result::getText);
    var workProgrammeResults = Arrays.stream(PearsWorkProgrammeComparison.Result.values())
        .map(PearsWorkProgrammeComparison.Result::getText);
    var results = Stream.concat(scheduleResults, workProgrammeResults)
        .distinct()
        .filter(result -> licences.stream().anyMatch(licence -> licence.notes().containsKey(resultNote(result))))
        .toList();

    var lines = new ArrayList<List<String>>();
    lines.add(Stream.of(Stream.of("Licence", "Status", "Attempts", "Outcome"), results.stream(), Stream.of("Notes"))
        .flatMap(column -> column)
        .toList());
    for (var licence : licences) {
      var row = new ArrayList<>(List.of(
          licence.licenceReference(),
          licence.status().name(),
          String.valueOf(licence.attempts()),
          Objects.toString(licence.outcome(), "")
      ));
      results.forEach(result -> row.add(Objects.toString(licence.notes().get(resultNote(result)), "")));
      row.add(mapperNotes(licence));
      lines.add(row);
    }

    return lines.stream()
        .map(line -> line.stream().map(PearsScheduleComparisonReport::csvCell).collect(Collectors.joining(",")))
        .collect(Collectors.joining(LINE_END, "", LINE_END));
  }

  private static String resultNote(String result) {
    return PearsScheduleValidationService.RESULT_NOTE_PREFIX + result;
  }

  private static String mapperNotes(PearsScheduleMigrationRunService.RunLicence licence) {
    return licence.notes().entrySet().stream()
        .filter(note -> !note.getKey().startsWith(PearsScheduleValidationService.RESULT_NOTE_PREFIX))
        .sorted(Map.Entry.comparingByKey())
        .map(note -> "%s: %d".formatted(note.getKey(), note.getValue()))
        .collect(Collectors.joining("; "));
  }
}
