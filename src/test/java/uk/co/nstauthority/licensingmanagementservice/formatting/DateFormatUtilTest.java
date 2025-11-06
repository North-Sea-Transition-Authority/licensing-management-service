package uk.co.nstauthority.licensingmanagementservice.formatting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class DateFormatUtilTest {

  @Test
  void testFormatFromLocalDate() {
    var localDate = LocalDate.of(1990, 1, 1);
    var formattedLocalDate = DateFormatUtil.convertToDisplayText(localDate);
    assertEquals("1 January 1990", formattedLocalDate);
  }

  @Test
  void testFormatFromInstantWithTime() {
    var instant = LocalDateTime.of(1990, 1, 1, 10, 6, 30)
        .atZone(ZoneId.systemDefault())
        .toInstant();
    var formattedInstantWithTime = DateFormatUtil.convertToDisplayTextWithTime(instant);
    assertEquals("1 January 1990 10:06:30", formattedInstantWithTime);
  }
}
