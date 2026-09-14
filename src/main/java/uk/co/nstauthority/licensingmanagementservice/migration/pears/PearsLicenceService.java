package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Reads what PEARS holds for a licence out of the PEARS database.
 */
@Service
@ConditionalOnPearsDataSource
class PearsLicenceService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsLicenceService.class);

  private static final String LIVE_POSITIONS_SQL = sql("live-positions.sql");
  private static final String LICENCE_REFERENCES_SQL = sql("licence-references.sql");
  private static final String DATA_POINT_POSITIONS_SQL = sql("data-point-positions.sql");

  private final DataSource dataSource;

  PearsLicenceService(@Qualifier("pearsDataSource") DataSource pearsDataSource) {
    this.dataSource = pearsDataSource;
  }

  /**
   * The live positions PEARS holds for one licence, in the order the licence holds them.
   *
   * @param licenceType   the licence's prefix, for example {@code P}
   * @param licenceNumber the licence's number, for example {@code 1}
   */
  public PearsLicencePositions getLicencePositions(String licenceType, int licenceNumber) {
    var positionRows = queryPositionRows(LIVE_POSITIONS_SQL, "live positions", licenceType, licenceNumber);
    if (positionRows.isEmpty()) {
      return new PearsLicencePositions(licenceType, licenceNumber, List.of());
    }

    return PearsLicencePositions.reconstruct(positionRows);
  }

  /**
   * The positions PEARS itself holds for one licence, taken from its own data points.
   *
   * <p>An oracle independent of {@link #getLicencePositions}, which re-derives the positions from the
   * operations that made them. The migration builds a licence from the operations, so checking the
   * result against them again would only restate the reading; the data points are what PEARS holds.
   *
   * @param licenceType   the licence's prefix, for example {@code P}
   * @param licenceNumber the licence's number, for example {@code 1}
   */
  public PearsLicencePositions dataPointPositions(String licenceType, int licenceNumber) {
    var rows = queryPositionRows(DATA_POINT_POSITIONS_SQL, "data point positions", licenceType, licenceNumber);
    if (rows.isEmpty()) {
      return new PearsLicencePositions(licenceType, licenceNumber, List.of());
    }

    return PearsLicencePositions.reconstruct(rows);
  }

  /**
   * Every licence the sweep should compare, in licence order: the licences PEARS holds data
   * points for, together with the licences its operations name.
   */
  public List<PearsLicenceReference> licenceReferences() {
    var start = System.nanoTime();
    var references = new ArrayList<PearsLicenceReference>();

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(LICENCE_REFERENCES_SQL)) {
      statement.setFetchSize(5_000);

      try (var resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          references.add(new PearsLicenceReference(
              resultSet.getString(1), // licence_type
              resultSet.getInt(2) // licence_no
          ));
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not read the licences to compare from PEARS", e);
    }

    LOGGER.info("Found {} licences in PEARS in {}ms",
        references.size(), Duration.ofNanos(System.nanoTime() - start).toMillis());
    return references;
  }

  /**
   * Both position queries return the same five columns -- licence, regulator reference, position
   * date and the position's sequence within that date -- so they are read the same way.
   */
  private List<PearsLicencePositions.Row> queryPositionRows(
      String sql,
      String description,
      String licenceType,
      int licenceNumber
  ) {
    var start = System.nanoTime();
    var rows = new ArrayList<PearsLicencePositions.Row>();

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(sql)) {
      statement.setFetchSize(5_000);
      statement.setString(1, licenceType);
      statement.setInt(2, licenceNumber);

      try (var resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          rows.add(new PearsLicencePositions.Row(
              resultSet.getString(1), // licence_type
              resultSet.getInt(2), // licence_no
              resultSet.getString(3), // regulator_reference_full
              LocalDate.parse(resultSet.getString(4)), // position_date
              resultSet.getInt(5) // position_sequence
          ));
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException(
          "Could not read %s for %s%d".formatted(description, licenceType, licenceNumber), e);
    }

    LOGGER.info("Processed {} {} rows from PEARS in {}ms",
        rows.size(), description, Duration.ofNanos(System.nanoTime() - start).toMillis());
    return rows;
  }

  private static String sql(String name) {
    var resource = new ClassPathResource("pears-migration/" + name);
    try (var inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read SQL resource " + resource, e);
    }
  }

}
