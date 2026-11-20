package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
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
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * The comparison report for chosen licences (plan, design section 6), as a page or a CSV download. Accepts GET as well
 * as POST, like the migration's other endpoints, so it can be opened by pasting the URL into a logged in browser.
 */
@Profile("migration")
@ConditionalOnPearsDataSource
@RestController
@RequestMapping("/migration/pears-schedules/compare")
public class PearsScheduleComparisonController {

  static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

  private static final String NO_LICENCES = "No licences given to compare";
  private static final String ALL = "all";

  private final PearsScheduleComparisonService comparisonService;

  PearsScheduleComparisonController(PearsScheduleComparisonService comparisonService) {
    this.comparisonService = comparisonService;
  }

  /**
   * Compares the licences given.
   *
   * @param licences licence references separated by commas, for example {@code P1556,PEDL236}, or {@code all} for every
   *                 licence the migration wrote a schedule for
   * @param format   {@code csv} for a download; otherwise a page
   * @param versions {@code all} for every version, oldest first; otherwise PEARS's current version against LMS's active
   *                 detail
   * @param issues   {@code only} for just the licences with an issue to look into
   */
  @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> compare(
      @RequestParam(required = false) @Nullable String licences,
      @RequestParam(required = false) @Nullable String format,
      @RequestParam(required = false) @Nullable String versions,
      @RequestParam(required = false) @Nullable String issues
  ) {
    var csv = isOption(format, "csv");
    var allVersions = isOption(versions, ALL);
    var onlyIssues = isOption(issues, "only");
    var everyMigratedLicence = licences != null && licences.trim().equalsIgnoreCase(ALL);
    var references = everyMigratedLicence ? comparisonService.migratedLicences() : licenceReferences(licences);

    var compared = comparisonService.compare(references, allVersions);
    var comparisons = onlyIssues ? compared.stream().filter(PearsScheduleComparison::hasIssues).toList() : compared;

    if (csv) {
      return ResponseEntity.ok()
          .contentType(TEXT_CSV)
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pears-schedule-comparison.csv\"")
          .body(PearsScheduleComparisonReport.csv(comparisons));
    }
    return ResponseEntity.ok()
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .body(PearsScheduleComparisonReport.html(comparisons, scope(compared, comparisons, onlyIssues)));
  }

  private static String scope(
      List<PearsScheduleComparison> compared,
      List<PearsScheduleComparison> shown,
      boolean onlyIssues
  ) {
    if (onlyIssues) {
      return "%d licences compared, %d with issues to look into: only those are shown".formatted(
          compared.size(), shown.size());
    }
    return "%d licences compared".formatted(compared.size());
  }

  /**
   * Returned for a licence reference that isn't one, or an option the report doesn't have.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
  }

  private static List<PearsLicenceReference> licenceReferences(@Nullable String licences) {
    if (licences == null) {
      throw new IllegalArgumentException(NO_LICENCES);
    }
    var references = Arrays.stream(licences.split(","))
        .map(StringUtils::trimToNull)
        .filter(StringUtils::isNotEmpty)
        .map(reference -> reference.toUpperCase(Locale.ROOT))
        .distinct()
        .map(PearsLicenceReference::parse)
        .toList();
    if (references.isEmpty()) {
      throw new IllegalArgumentException(NO_LICENCES);
    }
    return references;
  }

  private static boolean isOption(@Nullable String value, String option) {
    if (value == null || value.isBlank()) {
      return false;
    }
    if (!value.trim().equalsIgnoreCase(option)) {
      throw new IllegalArgumentException("%s is not an option of the comparison report".formatted(value));
    }
    return true;
  }
}
