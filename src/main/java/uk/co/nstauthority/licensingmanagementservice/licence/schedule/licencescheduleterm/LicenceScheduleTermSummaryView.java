package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

public record LicenceScheduleTermSummaryView(String duration, String startDate, String endDate) {

  public static LicenceScheduleTermSummaryView fromTerm(LicenceScheduleTerm term) {
    return new LicenceScheduleTermSummaryView(
        ThreeFieldDurationDisplayUtil.convertToDisplayText(term.getTermDuration()),
        DateFormatUtil.convertToDisplayText(term.getStartDate()),
        DateFormatUtil.convertToDisplayText(term.getEndDate())
    );
  }

}
