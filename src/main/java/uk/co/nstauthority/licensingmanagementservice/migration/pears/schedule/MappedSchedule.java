package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

/**
 * A licence's PEARS schedule mapped to what its LMS schedule will hold, ready to be written.
 *
 * @param licence         the licence
 * @param versions        every PEARS version that could be mapped, oldest first, each to become an LMS schedule detail
 * @param leftOutVersions the historical versions that could not be mapped, oldest first, for the migration report
 * @param notes           what the mapper decided along the way, counted by reason, for the migration report
 */
public record MappedSchedule(
    PearsLicenceReference licence,
    List<MappedScheduleVersion> versions,
    List<LeftOutVersion> leftOutVersions,
    MigrationNotes notes
) {

  public MappedSchedule {
    versions = List.copyOf(versions);
    leftOutVersions = List.copyOf(leftOutVersions);
  }

  /**
   * The version to become the active LMS schedule detail.
   */
  public MappedScheduleVersion activeVersion() {
    return versions.stream()
        .filter(version -> version.status() == LicenceScheduleDetailStatus.ACTIVE)
        .findFirst()
        .orElseThrow();
  }

  /**
   * A historical PEARS version left out of the licence's LMS history because it breaks a rule the mapper applies. The
   * active version is never left out: if it can't be mapped, the licence isn't migrated.
   *
   * @param plsdId the PEARS version ({@code ped_licence_schedule_details.id})
   * @param reason the rule it breaks
   */
  public record LeftOutVersion(long plsdId, String reason) {
  }
}
