package uk.co.nstauthority.licensingmanagementservice.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalyticsConfiguration {
  private final AnalyticsConfigurationProperties analyticsConfigurationProperties;

  @Autowired
  AnalyticsConfiguration(AnalyticsConfigurationProperties analyticsConfigurationProperties) {
    this.analyticsConfigurationProperties = analyticsConfigurationProperties;
  }

  public AnalyticsConfigurationProperties getAnalyticsConfigurationProperties() {
    return analyticsConfigurationProperties;
  }
}
