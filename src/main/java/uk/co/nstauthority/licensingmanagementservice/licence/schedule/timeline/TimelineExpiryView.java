package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record TimelineExpiryView(
    String title,
    LocalDate expiryDate,
    String expiryDateString,
    String updateUrl,
    String deleteUrl
) implements ScheduleEvent {

  private static final String TITLE = "Licence expiry";

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.EXPIRY;
  }

  @Override
  public LocalDate getSortingDate() {
    return expiryDate;
  }

  public static ScheduleEvent getScheduleEventFrom(LicenceScheduleExpiry licenceScheduleExpiry) {
    return new TimelineExpiryView(
        TITLE,
        licenceScheduleExpiry.getExpiryDate(),
        DateFormatUtil.convertToDisplayText(licenceScheduleExpiry.getExpiryDate()),
        ReverseRouter.route(on(LicenceScheduleExpiryController.class)
            .renderUpdateLicenceExpiryPage(licenceScheduleExpiry.getId())),
        ""
    );
  }
}
