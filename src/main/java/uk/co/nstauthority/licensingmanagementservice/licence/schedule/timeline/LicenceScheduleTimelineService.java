package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;

@Service
public class LicenceScheduleTimelineService {

  private final LicenceStartDateService licenceStartDateService;

  public LicenceScheduleTimelineService(LicenceStartDateService licenceStartDateService) {
    this.licenceStartDateService = licenceStartDateService;
  }

  TimelineSummaryCardView getTimelineSummaryCardView(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);

    return new TimelineSummaryCardView(DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()));
  }
}
