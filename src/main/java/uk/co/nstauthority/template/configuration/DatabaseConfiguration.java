package uk.co.nstauthority.template.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.PostgreSqlJdbcIndexedSessionRepositoryCustomizer;

@Configuration
public class DatabaseConfiguration {

  @Bean
  public PostgreSqlJdbcIndexedSessionRepositoryCustomizer sessionRepositoryCustomizer() {
    return new PostgreSqlJdbcIndexedSessionRepositoryCustomizer();
  }

}
