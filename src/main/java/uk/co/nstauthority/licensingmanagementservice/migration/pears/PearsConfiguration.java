package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnPearsDataSource
class PearsConfiguration {

  // These are still needed, otherwise Spring doesn't set these up because it sees the Oracle pears.* beans below
  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource")
  public DataSourceProperties primaryDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource.hikari")
  public DataSource primaryDataSource(DataSourceProperties primaryDataSourceProperties) {
    return primaryDataSourceProperties
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }

  @Bean
  @ConfigurationProperties("pears.datasource")
  DataSourceProperties oracleDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @ConfigurationProperties("pears.datasource.hikari")
  DataSource oracleDataSource(@Qualifier("oracleDataSourceProperties") DataSourceProperties oracleDatasourceProperties) {
    return oracleDatasourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

}
