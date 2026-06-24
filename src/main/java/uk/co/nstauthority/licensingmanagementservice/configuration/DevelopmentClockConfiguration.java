package uk.co.nstauthority.licensingmanagementservice.configuration;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("development")
@Configuration
class DevelopmentClockConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(DevelopmentClockConfiguration.class);

  @Bean
  Clock clock(@Value("${lms.development.clock-override-date:}") String clockOverrideDate) {
    if (StringUtils.isBlank(clockOverrideDate)) {
      return Clock.systemDefaultZone();
    }
    var overrideDate = LocalDate.parse(clockOverrideDate.strip());
    LOGGER.warn("Development clock override is active — application 'now' is fixed to {}. " +
        "Unset LMS_DEVELOPMENT_CLOCK_OVERRIDE_DATE to restore the real system clock.", overrideDate);
    var zone = ZoneId.systemDefault();
    return Clock.fixed(
        overrideDate.atStartOfDay(zone).toInstant(),
        zone
    );
  }
}
