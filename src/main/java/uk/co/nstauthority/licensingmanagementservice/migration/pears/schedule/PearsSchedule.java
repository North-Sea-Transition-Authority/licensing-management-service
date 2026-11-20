package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.util.List;
import java.util.Optional;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * One licence's schedule as PEARS holds it: every version it has saved, oldest first.
 *
 * @param licence  the licence the schedule belongs to
 * @param versions every saved version, oldest first; empty for a licence PEARS holds no schedule for
 */
public record PearsSchedule(PearsLicenceReference licence, List<PearsScheduleVersion> versions) {

  public PearsSchedule {
    versions = List.copyOf(versions);
  }

  /**
   * The version PEARS shows today: the one with no end date. Empty when every version has been end-dated, which the
   * migration treats as having no current version.
   */
  public Optional<PearsScheduleVersion> currentVersion() {
    return versions.stream()
        .filter(PearsScheduleVersion::isCurrent)
        .reduce((first, second) -> {
          throw new IllegalStateException("%s has more than one current schedule version: %d and %d"
              .formatted(licence, first.plsdId(), second.plsdId()));
        });
  }
}
