package uk.co.nstauthority.licensingmanagementservice.configuration;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT1H")
class ShedlockConfiguration {

  @Bean
  LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(jdbcTemplate)
            .withTableName("lms.shedlock")
            .usingDbTime()
            .build()
    );
  }
}