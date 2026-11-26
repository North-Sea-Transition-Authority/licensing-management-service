package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import com.zaxxer.hikari.HikariDataSource;

/**
 * The PEARS database the {@code pears-migration} tests read, from {@code PEARS_DATASOURCE_URL},
 * {@code PEARS_DATASOURCE_USERNAME} and {@code PEARS_DATASOURCE_PASSWORD}. Read-only, with a small pool.
 */
public final class PearsTestDataSource {

  private PearsTestDataSource() {
  }

  public static HikariDataSource connect() {
    var dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(System.getenv("PEARS_DATASOURCE_URL"));
    dataSource.setUsername(System.getenv("PEARS_DATASOURCE_USERNAME"));
    dataSource.setPassword(System.getenv("PEARS_DATASOURCE_PASSWORD"));
    dataSource.setReadOnly(true);
    dataSource.setMaximumPoolSize(2);
    return dataSource;
  }
}
