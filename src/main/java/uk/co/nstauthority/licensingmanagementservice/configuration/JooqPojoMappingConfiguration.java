package uk.co.nstauthority.licensingmanagementservice.configuration;

import org.jooq.jpa.extensions.DefaultAnnotatedPojoMemberProvider;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JooqPojoMappingConfiguration {

  @Bean
  DefaultConfigurationCustomizer annotatedPojoMemberProviderCustomizer() {
    return configuration -> configuration.set(new DefaultAnnotatedPojoMemberProvider());
  }
}
