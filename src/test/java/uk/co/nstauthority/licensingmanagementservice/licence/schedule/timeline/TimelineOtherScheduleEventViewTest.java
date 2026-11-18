package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;

@ExtendWith(MockitoExtension.class)
class TimelineOtherScheduleEventViewTest {

  @ParameterizedTest
  @CsvSource({
      "50.0, 50%",
      "87.5, 87.5%",
      "0.0, 0%",
      "100.0, 100%"
  })
  void getScheduleEventFrom_relinquishmentPercentage_isShownWithoutTrailingZeros(String percentage, String shown) {
    var event = mandatoryRelinquishment();
    event.setRelinquishmentPercentage(new BigDecimal(percentage));

    var view = (TimelineOtherScheduleEventView) TimelineOtherScheduleEventView.getScheduleEventFrom(
        event, List.of(), new HashMap<>(), LocalDate.of(2026, Month.NOVEMBER, 17));

    assertThat(view.relinquishmentPercentage()).isEqualTo(shown);
  }

  @Test
  void getScheduleEventFrom_noRelinquishmentPercentage_isNull() {
    var view = (TimelineOtherScheduleEventView) TimelineOtherScheduleEventView.getScheduleEventFrom(
        mandatoryRelinquishment(), List.of(), new HashMap<>(), LocalDate.of(2026, Month.NOVEMBER, 17));

    assertThat(view.relinquishmentPercentage()).isNull();
  }

  private static OtherScheduleEvent mandatoryRelinquishment() {
    var event = new OtherScheduleEvent();
    event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    event.setDescription("50% Relinquishment");
    event.setEventDate(LocalDate.of(2018, Month.DECEMBER, 1));
    return event;
  }
}
