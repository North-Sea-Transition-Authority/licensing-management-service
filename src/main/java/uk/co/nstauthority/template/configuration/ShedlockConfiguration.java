package uk.co.nstauthority.template.configuration;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class ShedlockConfiguration {

  @Bean
  LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(jdbcTemplate)
            .withTableName("xyz.shedlock")
            .usingDbTime()
            .build()
    );
  }
}