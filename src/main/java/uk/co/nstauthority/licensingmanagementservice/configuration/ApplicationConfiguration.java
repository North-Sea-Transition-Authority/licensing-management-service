package uk.co.nstauthority.licensingmanagementservice.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ApplicationConfiguration {

  @Bean
  @Profile("!development")
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
