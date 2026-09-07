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
  public LivePositions livePositions(String licenceType, int licenceNumber) {
    var livePositions = LivePositions.reconstruct(queryLivePositionRows(licenceType, licenceNumber));
    LOGGER.info("Found {} live positions for licence {}", livePositions.positions().size(), livePositions.licenceReference());
    return livePositions;
  }

  private List<LivePositions.Row> queryLivePositionRows(String licenceType, int licenceNumber) {
    var start = System.nanoTime();
    var rows = new ArrayList<LivePositions.Row>();

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(LIVE_POSITIONS_SQL)) {
      statement.setFetchSize(5_000);
      statement.setString(1, licenceType);
      statement.setInt(2, licenceNumber);

      try (var resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          rows.add(new LivePositions.Row(
              resultSet.getString(1), // xpo.licence_type
              resultSet.getInt(2), // xpo.licence_no
              resultSet.getString(3), // xpt.regulator_reference_full
              LocalDate.parse(resultSet.getString(4)), // TO_CHAR(xpt.position_datetime, 'YYYY-MM-DD') position_date
              resultSet.getInt(5) // pst.position_sequence
          ));
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not read live positions for %s%d".formatted(licenceType, licenceNumber), e);
    }

    LOGGER.info("Processed {} rows from PEARS in {}ms",
        rows.size(), Duration.ofNanos(System.nanoTime() - start).toMillis());
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
