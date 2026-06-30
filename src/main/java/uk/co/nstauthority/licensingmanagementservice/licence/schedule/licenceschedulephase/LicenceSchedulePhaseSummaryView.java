package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

public record LicenceSchedulePhaseSummaryView(
    String duration,
    String startDate,
    String endDate
) {

  public static LicenceSchedulePhaseSummaryView fromPhase(LicenceSchedulePhase phase) {
    return new LicenceSchedulePhaseSummaryView(
        ThreeFieldDurationDisplayUtil.convertToDisplayText(phase.getPhaseDuration()),
        DateFormatUtil.convertToDisplayText(phase.getStartDate()),
        DateFormatUtil.convertToDisplayText(phase.getEndDate())
    );
  }

}
