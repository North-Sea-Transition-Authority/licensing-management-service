package uk.co.nstauthority.licensingmanagementservice.licence;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class DurationInputMapper {

  private DurationInputMapper() {

  }

  public static void mapToFormDuration(ThreeFieldDurationInput formDuration, ThreeFieldDuration requestDuration) {
    if (formDuration == null || requestDuration == null) {
      return;
    }

    formDuration.setDays(requestDuration.days() != null ? requestDuration.days().toString() : null);
    formDuration.setMonths(requestDuration.months() != null ? requestDuration.months().toString() : null);
    formDuration.setYears(requestDuration.years() != null ? requestDuration.years().toString() : null);
  }
}