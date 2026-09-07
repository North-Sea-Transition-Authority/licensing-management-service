package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * PEARS and the gis-framework GIS migration each connect to their own Oracle schema with their own
 * credentials, so each needs its own datasource. Both once named their beans {@code oracleDataSource}
 * and {@code oracleDataSourceProperties}, which stopped the application starting whenever PEARS
 * credentials and the {@code gis-migration} profile were present together.
 *
 * <p>This boots the context with both, so a future clash between the two fails the build.
 *
 * <p>Both Oracle datasources are {@code defaultCandidate = false}, so Spring Boot's
 * {@code DataSourceAutoConfiguration} still supplies the application's own {@code dataSource} and
 * nothing has to be marked {@code @Primary} to disambiguate them.
 *
 * <p>Neither Oracle datasource is ever connected to: the properties below describe unreachable hosts,
 * and {@code pears.datasource.hikari.initialization-fail-timeout=-1} keeps Hikari from probing at startup.
 */
@IntegrationTest
@ActiveProfiles({"development", "integration-test", "gis-migration"})
@TestPropertySource(properties = {
    "pears.datasource.url=jdbc:postgresql://pears.invalid:5432/pears",
    "pears.datasource.username=pears-never-connected-to",
    "pears.datasource.password=never-connected-to",
    "pears.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.oracle.url=jdbc:postgresql://gis.invalid:5432/gis",
    "spring.datasource.oracle.username=gis-never-connected-to",
    "spring.datasource.oracle.password=never-connected-to",
    "spring.datasource.oracle.driver-class-name=org.postgresql.Driver"
})
class PearsAndGisOracleDatasourceIntegrationTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void pearsAndGisMigrationEachBindTheirOwnDatasourceProperties() {
    var pearsProperties = applicationContext.getBean("pearsDataSourceProperties", DataSourceProperties.class);
    var gisOracleProperties = applicationContext.getBean("oracleDataSourceProperties", DataSourceProperties.class);

    assertThat(List.of(pearsProperties, gisOracleProperties))
        .extracting(DataSourceProperties::getUrl, DataSourceProperties::getUsername)
        .containsExactly(
            tuple("jdbc:postgresql://pears.invalid:5432/pears", "pears-never-connected-to"),
            tuple("jdbc:postgresql://gis.invalid:5432/gis", "gis-never-connected-to")
        );
  }

  @Test
  void theApplicationsOwnDatasourceIsTheAutoConfiguredOne() {
    var injectedByType = applicationContext.getBean(DataSource.class);

    assertThat(applicationContext.getBeanNamesForType(DataSource.class))
        .containsExactlyInAnyOrder("dataSource", "pearsDataSource", "oracleDataSource");
    assertThat(injectedByType).isSameAs(applicationContext.getBean("dataSource", DataSource.class));
  }

  @Test
  void pearsAndGisMigrationEachGetTheirOwnDatasource() {
    var pearsDataSource = applicationContext.getBean("pearsDataSource", DataSource.class);
    var gisOracleDataSource = applicationContext.getBean("oracleDataSource", DataSource.class);

    assertThat(pearsDataSource).isNotSameAs(gisOracleDataSource);
  }
}
