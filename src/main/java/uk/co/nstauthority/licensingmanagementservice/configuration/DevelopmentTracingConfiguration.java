package uk.co.nstauthority.licensingmanagementservice.configuration;

import brave.sampler.Sampler;
import io.micrometer.observation.ObservationPredicate;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

@Profile("development")
@Configuration
class DevelopmentTracingConfiguration {

  private static final ThreadLocal<Boolean> IS_HTTP_REQUEST = new ThreadLocal<>();

  @Bean
  FilterRegistrationBean<Filter> httpRequestMarkerFilter() {
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
    registration.setFilter((req, res, chain) -> {
      IS_HTTP_REQUEST.set(Boolean.TRUE);
      try {
        chain.doFilter(req, res);
      } finally {
        IS_HTTP_REQUEST.remove();
      }
    });
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Bean
  Sampler httpRequestOnlySampler(@Value("${LMS_ENABLE_ZIPKIN:0.0}") double samplingProbability) {
    if (samplingProbability <= 0.0) {
      return Sampler.NEVER_SAMPLE;
    }
    return new Sampler() {
      @Override
      public boolean isSampled(long traceId) {
        return Boolean.TRUE.equals(IS_HTTP_REQUEST.get());
      }
    };
  }

  @Bean
  ObservationPredicate noBackgroundTaskObservations() {
    return (name, context) -> !"spring.scheduled.tasks".equals(name)
        && !"spring.task.execution".equals(name);
  }

}
