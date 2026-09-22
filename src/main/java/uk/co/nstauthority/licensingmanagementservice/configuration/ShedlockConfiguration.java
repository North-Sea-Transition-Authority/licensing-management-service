package uk.co.nstauthority.licensingmanagementservice.configuration;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT1H")
class ShedlockConfiguration {

  /**
   * ShedLock writes its lock rows in their own transaction, so that a lock is visible to other instances before the
   * locked task has finished.
   *
   * <p>The application's transaction manager is handed over explicitly. Left to itself, ShedLock builds a second
   * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager} over the same {@link javax.sql.DataSource},
   * and its {@code REQUIRES_NEW} then has no knowledge of the JPA transaction a caller is already in - so rather than
   * suspending it, ShedLock takes the connection that transaction is bound to and commits it, committing whatever the
   * caller had written so far along with the lock row.</p>
   */
  @Bean
  LockProvider lockProvider(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(jdbcTemplate)
            .withTableName("lms.shedlock")
            .usingDbTime()
            .withTransactionManager(transactionManager)
            .build()
    );
  }
}
