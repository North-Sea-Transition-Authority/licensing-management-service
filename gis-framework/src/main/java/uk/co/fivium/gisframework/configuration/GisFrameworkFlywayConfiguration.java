package uk.co.fivium.gisframework.configuration;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
class GisFrameworkFlywayConfiguration {

  private static final String TABLE_NAME = "gis_framework_flyway_schema_history";
  // The migrations location must be different to the default, otherwise the migrations will also be applied to the
  // schema of apps using the starter as they will be shaded into the same location as the apps migrations in the jar.
  private static final String MIGRATIONS_LOCATION = "classpath:db/gis-framework-migration";

  @Autowired
  GisFrameworkFlywayConfiguration(
      DataSource dataSource,
      @Value("${spring.flyway.schemas}") String[] schemas
  ) {
    Flyway.configure()
        .dataSource(dataSource)
        .schemas(schemas)
        .table(TABLE_NAME)
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .locations(MIGRATIONS_LOCATION)
        .load()
        .migrate();
  }
}
