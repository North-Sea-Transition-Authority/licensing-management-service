package uk.co.nstauthority.licensingmanagementservice.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DevelopmentClockConfigurationTest {

  private DevelopmentClockConfiguration developmentClockConfiguration;

  @BeforeEach
  void setUp() {
    developmentClockConfiguration = new DevelopmentClockConfiguration();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void clock_whenOverrideDateIsBlank_returnsSystemDefaultZoneClock(String overrideDate) {
    var clock = developmentClockConfiguration.clock(overrideDate);

    assertThat(clock).isEqualTo(Clock.systemDefaultZone());
  }

  @Test
  void clock_whenOverrideDateIsValidIsoDate_returnsFixedClockAtMidnightOnThatDate() {
    var clock = developmentClockConfiguration.clock("2027-03-15");

    var expectedInstant = LocalDate.of(2027, 3, 15)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant();

    assertThat(clock.instant()).isEqualTo(expectedInstant);
    assertThat(clock.getZone()).isEqualTo(ZoneId.systemDefault());
  }

  @Test
  void clock_whenOverrideDateIsMalformed_throwsDateTimeParseException() {
    assertThatThrownBy(() -> developmentClockConfiguration.clock("not-a-date"))
        .isInstanceOf(DateTimeParseException.class);
  }
}
