package uk.co.nstauthority.licensingmanagementservice.components.duration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ThreeFieldDurationTest {

  @Test
  void total_whenNoDurations_assertZero() {
    assertThat(ThreeFieldDuration.total(List.of()))
        .isEqualTo(new ThreeFieldDuration(0, 0, 0));
  }

  @Test
  void total_whenSingleDuration_assertUnchanged() {
    assertThat(ThreeFieldDuration.total(List.of(new ThreeFieldDuration(1, 2, 3))))
        .isEqualTo(new ThreeFieldDuration(1, 2, 3));
  }

  @Test
  void total_whenYearsMonthsAndDays_assertSummedPerUnit() {
    assertThat(ThreeFieldDuration.total(List.of(
        new ThreeFieldDuration(1, 2, 3),
        new ThreeFieldDuration(2, 3, 4))))
        .isEqualTo(new ThreeFieldDuration(3, 5, 7));
  }

  @Test
  void total_whenMonthsSumToTwelve_assertRolledIntoYears() {
    assertThat(ThreeFieldDuration.total(List.of(
        new ThreeFieldDuration(0, 6, 0),
        new ThreeFieldDuration(0, 6, 0))))
        .isEqualTo(new ThreeFieldDuration(1, 0, 0));
  }

  @Test
  void total_whenMonthsSumAboveTwelve_assertRemainderLeftInMonths() {
    assertThat(ThreeFieldDuration.total(List.of(
        new ThreeFieldDuration(0, 7, 0),
        new ThreeFieldDuration(0, 6, 0))))
        .isEqualTo(new ThreeFieldDuration(1, 1, 0));
  }

  @Test
  void total_whenMonthsSumWellAboveTwentyFour_assertRolledIntoMultipleYears() {
    assertThat(ThreeFieldDuration.total(List.of(
        new ThreeFieldDuration(0, 20, 0),
        new ThreeFieldDuration(0, 20, 0))))
        .isEqualTo(new ThreeFieldDuration(3, 4, 0));
  }

  @Test
  void total_whenExistingYearsAndMonthsRollOver_assertYearsAccumulate() {
    assertThat(ThreeFieldDuration.total(List.of(
        new ThreeFieldDuration(1, 8, 0),
        new ThreeFieldDuration(2, 8, 0))))
        .isEqualTo(new ThreeFieldDuration(4, 4, 0));
  }

  @Test
  void total_whenDaysSumAboveThirty_assertDaysNotRolledIntoMonths() {
    assertThat(ThreeFieldDuration.total(List.of(
        new ThreeFieldDuration(0, 0, 20),
        new ThreeFieldDuration(0, 0, 25))))
        .isEqualTo(new ThreeFieldDuration(0, 0, 45));
  }

  @Test
  void total_whenDaysSumToThirtyOne_assertNotTreatedAsOneMonth() {
    assertThat(ThreeFieldDuration.total(List.of(
        new ThreeFieldDuration(0, 0, 16),
        new ThreeFieldDuration(0, 0, 15))))
        .isEqualTo(new ThreeFieldDuration(0, 0, 31));
  }

  @Test
  void total_whenThirtyOneDays_assertNotEqualToOneMonth() {
    assertThat(ThreeFieldDuration.total(List.of(new ThreeFieldDuration(0, 0, 31))))
        .isNotEqualTo(ThreeFieldDuration.total(List.of(new ThreeFieldDuration(0, 1, 0))));
  }

  @Test
  void total_whenTwelveMonthsComparedToOneYear_assertEqual() {
    assertThat(ThreeFieldDuration.total(List.of(new ThreeFieldDuration(0, 12, 0))))
        .isEqualTo(ThreeFieldDuration.total(List.of(new ThreeFieldDuration(1, 0, 0))));
  }
}
