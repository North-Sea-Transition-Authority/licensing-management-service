package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparisonController.isOption;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparisonController.licenceReferences;

import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;

/**
 * The work programme comparison report for chosen licences (plan, design section 6), as a page or a CSV download, with
 * the schedule comparison's options. Accepts GET as well as POST, so it can be opened by pasting the URL into a logged
 * in browser.
 */
@Profile("migration")
@ConditionalOnPearsDataSource
@RestController
@RequestMapping("/migration/pears-work-programmes/compare")
public class PearsWorkProgrammeComparisonController {

  private final PearsWorkProgrammeComparisonService comparisonService;

  PearsWorkProgrammeComparisonController(PearsWorkProgrammeComparisonService comparisonService) {
    this.comparisonService = comparisonService;
  }

  /**
   * Compares the licences given.
   *
   * @param licences    licence references separated by commas, for example {@code P1556,PEDL236}, or {@code all} for
   *                    every licence the migration wrote a work programme for
   * @param format      {@code csv} for a download; otherwise a page
   * @param issues      {@code only} for just the licences with an issue to look into
   * @param notMigrated {@code only} for just the licences with an activity not migrated (the yellow rows); with
   *                    {@code issues}, the licences with either
   */
  @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> compare(
      @RequestParam(required = false) @Nullable String licences,
      @RequestParam(required = false) @Nullable String format,
      @RequestParam(required = false) @Nullable String issues,
      @RequestParam(required = false) @Nullable String notMigrated
  ) {
    var csv = isOption(format, "csv");
    var onlyIssues = isOption(issues, "only");
    var onlyNotMigrated = isOption(notMigrated, "only");
    var everyMigratedLicence = licences != null && licences.trim().equalsIgnoreCase("all");
    var references = everyMigratedLicence ? comparisonService.migratedLicences() : licenceReferences(licences);

    var compared = comparisonService.compareInParallel(references);
    var comparisons = onlyIssues || onlyNotMigrated
        ? compared.stream()
            .filter(comparison -> (onlyIssues && comparison.hasIssues())
                || (onlyNotMigrated && comparison.hasActivitiesNotMigrated()))
            .toList()
        : compared;

    if (csv) {
      return ResponseEntity.ok()
          .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pears-work-programme-comparison.csv\"")
          .body(PearsWorkProgrammeComparisonReport.csv(comparisons));
    }
    return ResponseEntity.ok()
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .body(PearsWorkProgrammeComparisonReport.html(
            comparisons, scope(compared, comparisons, onlyIssues, onlyNotMigrated)));
  }

  private static String scope(
      List<PearsWorkProgrammeComparison> compared,
      List<PearsWorkProgrammeComparison> shown,
      boolean onlyIssues,
      boolean onlyNotMigrated
  ) {
    if (!onlyIssues && !onlyNotMigrated) {
      return "%d licences compared".formatted(compared.size());
    }
    String kept;
    if (onlyIssues && onlyNotMigrated) {
      kept = "issues to look into or activities not migrated";
    } else if (onlyIssues) {
      kept = "issues to look into";
    } else {
      kept = "activities not migrated";
    }
    return "%d licences compared, %d with %s: only those are shown".formatted(compared.size(), shown.size(), kept);
  }

  /**
   * Returned for a licence reference that isn't one, or an option the report doesn't have. The message doesn't repeat
   * what was given.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.TEXT_PLAIN)
        .body("Not a licence reference or option of the comparison report");
  }
}
