package uk.co.nstauthority.licensingmanagementservice.hibernate;

import org.hibernate.cfg.JdbcSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig {

  @Bean
  public HibernatePropertiesCustomizer configureStatementInspector(HibernateQueryCounter hibernateQueryCounter) {
    return properties -> properties.put(JdbcSettings.STATEMENT_INSPECTOR, hibernateQueryCounter);
  }

}
