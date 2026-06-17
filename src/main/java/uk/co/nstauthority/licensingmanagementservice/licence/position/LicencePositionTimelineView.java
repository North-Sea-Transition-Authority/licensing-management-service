package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

public record LicencePositionTimelineView(
    String regulatorReference,
    LocalDate positionDate
) {

  public String getFormattedDate() {
    return DateUtil.formatLongDate(positionDate);
  }
}
