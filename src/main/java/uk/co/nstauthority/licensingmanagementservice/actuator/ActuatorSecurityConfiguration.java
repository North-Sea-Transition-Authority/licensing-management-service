package uk.co.nstauthority.licensingmanagementservice.actuator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
class ActuatorSecurityConfiguration {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  SecurityFilterChain actuatorSecurityFilterChain(
      HttpSecurity httpSecurity,
      ActuatorConfigurationProperties actuatorConfigurationProperties
  ) throws Exception {
    var actuatorFilter = new ActuatorAuthenticationFilter(actuatorConfigurationProperties);

    return httpSecurity
        .securityMatcher("/actuator/**")
        .authorizeHttpRequests(http -> http
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().hasRole("ACTUATOR"))
        .addFilterAfter(actuatorFilter, SecurityContextHolderFilter.class)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
        .build();
  }
}
