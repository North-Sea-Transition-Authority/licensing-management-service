package uk.co.nstauthority.template.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.fivium.feedbackmanagementservice.client.FeedbackClientService;

@Configuration
public class FeedbackConfiguration {

  private final FeedbackConfigurationProperties feedbackConfigurationProperties;

  @Autowired
  public FeedbackConfiguration(FeedbackConfigurationProperties feedbackConfigurationProperties) {
    this.feedbackConfigurationProperties = feedbackConfigurationProperties;
  }

  @Bean
  FeedbackClientService feedbackClientService(ObjectMapper objectMapper) {
    return new FeedbackClientService(
        objectMapper,
        feedbackConfigurationProperties.urlBase(),
        feedbackConfigurationProperties.connectionTimeoutSeconds(),
        feedbackConfigurationProperties.saveFeedbackUrl(),
        feedbackConfigurationProperties.serviceName(),
        feedbackConfigurationProperties.presharedKey()
    );
  }
}
