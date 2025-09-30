package uk.co.nstauthority.licensingmanagementservice.components.duration;

import static org.assertj.core.api.Assertions.assertThat;

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
}