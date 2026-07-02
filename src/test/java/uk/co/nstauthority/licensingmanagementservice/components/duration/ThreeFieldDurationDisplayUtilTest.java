package uk.co.nstauthority.licensingmanagementservice.components.duration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ThreeFieldDurationDisplayUtilTest {

  @Test
  void convertToDisplayText() {
    var duration = new ThreeFieldDuration(2, 2, 2);

    assertThat(ThreeFieldDurationDisplayUtil.convertToDisplayText(duration)).isEqualTo("2 years 2 months 2 days");
  }

  @Test
  void convertToDisplayText_singleYearMonthDay() {
    var duration = new ThreeFieldDuration(1, 1, 1);

    assertThat(ThreeFieldDurationDisplayUtil.convertToDisplayText(duration)).isEqualTo("1 year 1 month 1 day");
  }

  @Test
  void convertToDisplayText_years() {
    var duration = new ThreeFieldDuration(2, 0, 0);

    assertThat(ThreeFieldDurationDisplayUtil.convertToDisplayText(duration)).isEqualTo("2 years");
  }

  @Test
  void convertToDisplayText_months() {
    var duration = new ThreeFieldDuration(0, 2, 0);

    assertThat(ThreeFieldDurationDisplayUtil.convertToDisplayText(duration)).isEqualTo("2 months");
  }

  @Test
  void convertToDisplayText_days() {
    var duration = new ThreeFieldDuration(0, 0, 2);

    assertThat(ThreeFieldDurationDisplayUtil.convertToDisplayText(duration)).isEqualTo("2 days");
  }

  @Test
  void convertDatesToDurationDisplayText() {
    assertThat(ThreeFieldDurationDisplayUtil.convertDatesToDurationDisplayText(
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2022, 3, 3)
    )).isEqualTo("2 years 2 months 3 days");
  }

  @Test
  void convertDatesToDurationDisplayText_exactYears() {
    assertThat(ThreeFieldDurationDisplayUtil.convertDatesToDurationDisplayText(
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2022, 1, 1)
    )).isEqualTo("2 years 1 day");
  }

  @Test
  void convertDatesToDurationDisplayText_nullStartDate() {
    assertThat(ThreeFieldDurationDisplayUtil.convertDatesToDurationDisplayText(
        null,
        LocalDate.of(2022, 1, 1)
    )).isEmpty();
  }

  @Test
  void convertDatesToDurationDisplayText_nullEndDate() {
    assertThat(ThreeFieldDurationDisplayUtil.convertDatesToDurationDisplayText(
        LocalDate.of(2020, 1, 1),
        null
    )).isEmpty();
  }
}