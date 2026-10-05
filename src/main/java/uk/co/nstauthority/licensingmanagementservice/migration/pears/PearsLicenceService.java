package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.LicenceHistoryReader;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.LicenceOperationHistory;

/**
 * Reads what PEARS holds for a licence out of the PEARS database.
 */
@Service
@ConditionalOnPearsDataSource
class PearsLicenceService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsLicenceService.class);

  private static final String LICENCE_HISTORY_SQL = sql("licence-history.sql");
  private static final String LICENCE_REFERENCES_SQL = sql("licence-references.sql");
  private static final String DATA_POINT_POSITIONS_SQL = sql("data-point-positions.sql");

  /**
   * What an operation type must look like before it is interpolated into the history query. Oracle
   * has no list bind, so declared types go into the SQL as text and this is what makes that safe.
   */
  private static final Pattern OPERATION_TYPE = Pattern.compile("[A-Z_]{1,64}");

  private final DataSource dataSource;

  PearsLicenceService(@Qualifier("pearsDataSource") DataSource pearsDataSource) {
    this.dataSource = pearsDataSource;
  }

  /**
   * One licence's operation history: every operation of every executed transaction in the live
   * simulation, in the order PEARS made them. Only the operation types named here carry their XML.
   *
   * @param licenceType    the licence's prefix, for example {@code P}
   * @param licenceNumber  the licence's number, for example {@code 1}
   * @param operationTypes the PEARS OPERATION_TYPEs whose XML to fetch
   */
  public LicenceOperationHistory licenceHistory(String licenceType, int licenceNumber, Set<String> operationTypes) {
    var start = System.nanoTime();
    var sql = LICENCE_HISTORY_SQL.formatted(operationTypeList(operationTypes));

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(sql)) {
      // Every bind is a (licence type, licence number) pair, counted off the query so a new filter
      // cannot be missed. The file holds no question mark outside its binds.
      var binds = sql.chars().filter(character -> character == '?').count();
      for (var bind = 1; bind < binds; bind += 2) {
        statement.setString(bind, licenceType);
        statement.setInt(bind + 1, licenceNumber);
      }

      try (var resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return new LicenceOperationHistory(licenceType, licenceNumber, List.of());
        }

        // XMLAGG over no rows is null, which is a licence PEARS holds no operations for.
        var document = resultSet.getCharacterStream(1);
        var history = LicenceHistoryReader.read(document == null ? emptyHistory(licenceType, licenceNumber) : document);

        LOGGER.info("Read {} operations of {}{} from PEARS in {}ms", history.operations().size(),
            licenceType, licenceNumber, Duration.ofNanos(System.nanoTime() - start).toMillis());
        return history;
      }
    } catch (SQLException | IOException e) {
      throw new IllegalStateException(
          "Could not read the operation history for %s%d".formatted(licenceType, licenceNumber), e);
    }
  }

  /**
   * The positions PEARS itself holds for one licence, taken from its own data points. An oracle
   * independent of {@link #licenceHistory}, so it can catch the reading being wrong rather than only
   * the replay.
   *
   * @param licenceType   the licence's prefix, for example {@code P}
   * @param licenceNumber the licence's number, for example {@code 1}
   */
  public PearsLicencePositions dataPointPositions(String licenceType, int licenceNumber) {
    var rows = queryDataPointRows(licenceType, licenceNumber);
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

  private List<PearsLicencePositions.Row> queryDataPointRows(String licenceType, int licenceNumber) {
    var start = System.nanoTime();
    var rows = new ArrayList<PearsLicencePositions.Row>();

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(DATA_POINT_POSITIONS_SQL)) {
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
              resultSet.getInt(5), // position_sequence
              resultSet.getLong(6) // ped_tran_id
          ));
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException(
          "Could not read data point positions for %s%d".formatted(licenceType, licenceNumber), e);
    }

    LOGGER.info("Processed {} data point position rows from PEARS in {}ms",
        rows.size(), Duration.ofNanos(System.nanoTime() - start).toMillis());
    return rows;
  }

  /**
   * The declared operation types as an SQL list, rejecting anything that is not plainly a type name.
   */
  private static String operationTypeList(Set<String> operationTypes) {
    if (operationTypes.isEmpty()) {
      return "NULL";
    }

    return operationTypes.stream()
        .sorted()
        .map(operationType -> {
          if (!OPERATION_TYPE.matcher(operationType).matches()) {
            throw new IllegalArgumentException("Not a PEARS operation type: " + operationType);
          }
          return "'%s'".formatted(operationType);
        })
        .reduce((left, right) -> left + "," + right)
        .orElseThrow();
  }

  private static Reader emptyHistory(String licenceType, int licenceNumber) {
    return new StringReader(
        "<LICENCE_OPERATION_HISTORY licence_type=\"%s\" licence_no=\"%d\"/>"
            .formatted(licenceType, licenceNumber));
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
