package uk.co.fivium.gisframework.migration.oracle;

import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Profile("gis-migration")
@Configuration
@EnableJpaRepositories(
    basePackageClasses = OracleDatasourceConfiguration.class,
    entityManagerFactoryRef = "oracleEntityManagerFactory",
    transactionManagerRef = "oracleTransactionManager"
)
// https://docs.spring.io/spring-boot/how-to/data-access.html#howto.data-access.configure-two-datasources
// https://docs.spring.io/spring-boot/how-to/data-access.html#howto.data-access.use-multiple-entity-managers
class OracleDatasourceConfiguration {

  @Bean(defaultCandidate = false)
  @ConfigurationProperties("spring.datasource.oracle")
  DataSourceProperties oracleDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(defaultCandidate = false)
  DataSource oracleDataSource(@Qualifier("oracleDataSourceProperties") DataSourceProperties oracleDataSourceProperties) {
    return oracleDataSourceProperties
        .initializeDataSourceBuilder()
        .build();
  }

  @Bean(defaultCandidate = false)
  @ConfigurationProperties("spring.jpa.oracle")
  JpaProperties oracleJpaProperties() {
    return new JpaProperties();
  }

  @Bean(defaultCandidate = false)
  LocalContainerEntityManagerFactoryBean oracleEntityManagerFactory(
      @Qualifier("oracleDataSource") DataSource oracleDataSource,
      @Qualifier("oracleJpaProperties") JpaProperties oracleJpaProperties
  ) {
    var jpaPropertiesMap = new HibernateProperties().determineHibernateProperties(
        oracleJpaProperties.getProperties(),
        new HibernateSettings()
    );

    var builder = new EntityManagerFactoryBuilder(
        new HibernateJpaVendorAdapter(),
        dataSource -> jpaPropertiesMap,
        null
    );

    return builder
        .dataSource(oracleDataSource)
        .packages(OracleDatasourceConfiguration.class)
        .persistenceUnit("oracle")
        .build();
  }

  @Bean(defaultCandidate = false)
  PlatformTransactionManager oracleTransactionManager(
      @Qualifier("oracleEntityManagerFactory") LocalContainerEntityManagerFactoryBean oracleEntityManagerFactory
  ) {
    return new JpaTransactionManager(Objects.requireNonNull(oracleEntityManagerFactory.getObject()));
  }
}
