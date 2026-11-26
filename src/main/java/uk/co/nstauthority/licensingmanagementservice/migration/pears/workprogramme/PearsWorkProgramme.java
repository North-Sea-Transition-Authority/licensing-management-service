package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * One of a licence's work programmes as PEARS holds it today: the current version of a
 * {@code PED_WORK_PROGRAMME}. Values are the text PEARS stores, untrimmed and unparsed.
 *
 * @param pedWpdId the current version's {@code PED_WORK_PROGRAMME_DETAILS.ID}
 * @param start    when the current version was saved
 * @param type     {@code INITIAL}, {@code FALLOW} or {@code EXTENSION}
 */
public record PearsWorkProgramme(
    PearsLicenceReference licence,
    long pedWpId,
    long pedWpdId,
    LocalDateTime start,
    @Nullable String type,
    @Nullable String title,
    @Nullable String status,
    @Nullable Breakpoint breakpoint,
    List<PearsActivity> activities,
    List<PearsNote> notes
) {

  public PearsWorkProgramme {
    activities = List.copyOf(activities);
    notes = List.copyOf(notes);
  }

  /**
   * The drill-or-drop decision point, as an offset from the licence start.
   */
  public record Breakpoint(@Nullable String years, @Nullable String months, @Nullable String days) {

    @Nullable
    static Breakpoint of(@Nullable String years, @Nullable String months, @Nullable String days) {
      if (years == null && months == null && days == null) {
        return null;
      }
      return new Breakpoint(years, months, days);
    }
  }
}
