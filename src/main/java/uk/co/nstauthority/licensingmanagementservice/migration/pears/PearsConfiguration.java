package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnPearsDataSource
class PearsConfiguration {

  // named for PEARS rather than Oracle: gis-framework's OracleDatasourceConfiguration owns the
  // oracleDataSource* names for its own, separate Oracle schema
  @Bean(defaultCandidate = false)
  @ConfigurationProperties("pears.datasource")
  DataSourceProperties pearsDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(defaultCandidate = false)
  @ConfigurationProperties("pears.datasource.hikari")
  DataSource pearsDataSource(@Qualifier("pearsDataSourceProperties") DataSourceProperties pearsDataSourceProperties) {
    return pearsDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

}
