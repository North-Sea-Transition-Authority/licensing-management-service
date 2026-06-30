package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

public record LicenceScheduleRateSummaryView(
    String startDate,
    String rentalRate
) {

  public static LicenceScheduleRateSummaryView from(LicenceScheduleRate rate) {
    return new LicenceScheduleRateSummaryView(
        DateFormatUtil.convertToDisplayText(rate.getStartDate()),
        "£%s".formatted(rate.getRentalRate().toString())
    );
  }
}
