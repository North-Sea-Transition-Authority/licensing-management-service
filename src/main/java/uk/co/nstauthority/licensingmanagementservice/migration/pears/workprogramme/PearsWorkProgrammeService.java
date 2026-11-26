package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * Reads licence work programmes out of the PEARS database, for the work programme migration.
 */
@Service
@ConditionalOnPearsDataSource
public class PearsWorkProgrammeService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsWorkProgrammeService.class);

  private static final String LICENCE_REFERENCES_SQL = sql("work-programme-licence-references.sql");
  private static final String WORK_PROGRAMMES_SQL = sql("work-programmes.sql");
  private static final String ACTIVITIES_SQL = sql("work-programme-activities.sql");
  private static final String NOTES_SQL = sql("work-programme-notes.sql");

  /**
   * Everything the queries read, each with a query that reads it and returns nothing. The migration user needs a grant
   * on each, so a run checks them all before it starts rather than failing licence by licence.
   */
  static final List<ReadCheck> READ_CHECKS = List.of(
      new ReadCheck("pedmgr.ped_licence_master", "SELECT 1 FROM pedmgr.ped_licence_master WHERE 1 = 0"),
      new ReadCheck("pedmgr.ped_work_programme", "SELECT 1 FROM pedmgr.ped_work_programme WHERE 1 = 0"),
      new ReadCheck("pedmgr.ped_work_programme_details", "SELECT 1 FROM pedmgr.ped_work_programme_details WHERE 1 = 0")
  );

  private final DataSource dataSource;

  PearsWorkProgrammeService(@Qualifier("pearsDataSource") DataSource pearsDataSource) {
    this.dataSource = pearsDataSource;
  }

  /**
   * Checks the migration user can read everything the migration reads, naming every table it cannot.
   *
   * @throws MigrationPreconditionException if any of them cannot be read
   */
  public void checkReadable() {
    var unreadable = new ArrayList<String>();

    try (var connection = dataSource.getConnection()) {
      for (var readCheck : READ_CHECKS) {
        if (!canRead(connection, readCheck)) {
          unreadable.add(readCheck.pearsObject());
        }
      }
    } catch (SQLException e) {
      throw new MigrationPreconditionException("Could not connect to PEARS: " + e.getMessage());
    }

    if (!unreadable.isEmpty()) {
      throw new MigrationPreconditionException(
          "The PEARS migration user cannot read " + String.join(", ", unreadable));
    }
  }

  /**
   * Every licence the work programme migration covers, in licence order: the numbered licences with a current work
   * programme that isn't a draft, whose licence type maps to a type PEARS manages. Any other is logged and left out.
   */
  public List<PearsLicenceReference> workProgrammeLicenceReferences() {
    var start = System.nanoTime();
    var references = new ArrayList<PearsLicenceReference>();
    var leftOut = new ArrayList<PearsLicenceReference>();

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(LICENCE_REFERENCES_SQL)) {
      statement.setFetchSize(5_000);

      try (var resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          var reference = new PearsLicenceReference(
              resultSet.getString(1), // licence_type
              resultSet.getInt(2) // licence_no
          );
          (reference.isOfTypePearsManages() ? references : leftOut).add(reference);
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not read the licences with a work programme from PEARS", e);
    }

    if (!leftOut.isEmpty()) {
      LOGGER.warn("Left out {} licences with a PEARS work programme whose licence type PEARS does not manage in LMS: {}",
          leftOut.size(), leftOut);
    }

    LOGGER.info("Found {} licences with a work programme in PEARS in {}ms",
        references.size(), Duration.ofNanos(System.nanoTime() - start).toMillis());
    return references;
  }

  /**
   * One licence's current work programmes, apart from drafts, oldest programme first, each with its activities and
   * notes. The three queries run in one read-only transaction, so they see the same moment even if PEARS saves the
   * work programme meanwhile.
   *
   * @param licence the licence, for example {@code P2244}
   */
  public List<PearsWorkProgramme> licenceWorkProgrammes(PearsLicenceReference licence) {
    var start = System.nanoTime();
    List<PearsWorkProgramme> workProgrammes;

    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        try (var statement = connection.createStatement()) {
          statement.execute("SET TRANSACTION READ ONLY");
        }
        var activities = readActivities(connection, licence);
        var notes = readNotes(connection, licence);
        workProgrammes = readWorkProgrammes(connection, licence, activities, notes);
      } finally {
        connection.rollback();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not read the work programmes of %s".formatted(licence), e);
    }

    LOGGER.info("Read {} work programmes of {} from PEARS in {}ms",
        workProgrammes.size(), licence, Duration.ofNanos(System.nanoTime() - start).toMillis());
    return workProgrammes;
  }

  private static List<PearsWorkProgramme> readWorkProgrammes(
      Connection connection,
      PearsLicenceReference licence,
      Map<Long, List<PearsActivity>> activities,
      Map<Long, List<PearsNote>> notes
  ) throws SQLException {
    var workProgrammes = new ArrayList<PearsWorkProgramme>();

    forEachRow(connection, WORK_PROGRAMMES_SQL, licence, resultSet -> {
      var pedWpId = resultSet.getLong(1); // ped_wp_id
      workProgrammes.add(new PearsWorkProgramme(
          licence,
          pedWpId,
          resultSet.getLong(2), // ped_wpd_id
          LocalDateTime.parse(resultSet.getString(3)), // start_datetime
          resultSet.getString(4), // work_programme_type
          resultSet.getString(5), // title
          resultSet.getString(6), // work_programme_status
          PearsWorkProgramme.Breakpoint.of(
              resultSet.getString(7), // breakpoint_years
              resultSet.getString(8), // breakpoint_months
              resultSet.getString(9) // breakpoint_days
          ),
          activities.getOrDefault(pedWpId, List.of()),
          notes.getOrDefault(pedWpId, List.of())
      ));
    });

    return workProgrammes;
  }

  private static Map<Long, List<PearsActivity>> readActivities(Connection connection, PearsLicenceReference licence)
      throws SQLException {
    var activities = new HashMap<Long, List<PearsActivity>>();

    forEachRow(connection, ACTIVITIES_SQL, licence, resultSet -> {
      var pedWpId = resultSet.getLong(1); // ped_wp_id
      activities.computeIfAbsent(pedWpId, id -> new ArrayList<>()).add(new PearsActivity(
          resultSet.getInt(2), // position
          PearsActivity.ListKind.valueOf(resultSet.getString(3)), // list_kind
          resultSet.getString(4), // phase_type
          resultSet.getString(5), // activity_id
          resultSet.getString(6), // activity_type
          resultSet.getString(7), // title
          resultSet.getString(8), // activity_status
          resultSet.getString(9), // competed_blocks
          resultSet.getString(10), // description
          nullableDateTime(resultSet.getString(11)) // status_since
      ));
    });

    return activities;
  }

  private static Map<Long, List<PearsNote>> readNotes(Connection connection, PearsLicenceReference licence)
      throws SQLException {
    var notes = new HashMap<Long, List<PearsNote>>();

    forEachRow(connection, NOTES_SQL, licence, resultSet -> {
      var pedWpId = resultSet.getLong(1); // ped_wp_id
      notes.computeIfAbsent(pedWpId, id -> new ArrayList<>()).add(new PearsNote(
          resultSet.getInt(2), // position
          resultSet.getString(3), // note_type
          resultSet.getString(4), // activity_id
          resultSet.getString(5), // activity_title
          resultSet.getString(6), // new_activity_status
          nullableDateTime(resultSet.getString(7)), // note_datetime
          resultSet.getString(8), // created_by
          resultSet.getString(9) // description
      ));
    });

    return notes;
  }

  private static void forEachRow(Connection connection, String sql, PearsLicenceReference licence, RowReader rowReader)
      throws SQLException {
    try (var statement = connection.prepareStatement(sql)) {
      statement.setFetchSize(1_000);
      statement.setString(1, licence.licenceType());
      statement.setInt(2, licence.licenceNo());

      try (var resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          rowReader.read(resultSet);
        }
      }
    }
  }

  @FunctionalInterface
  private interface RowReader {
    void read(ResultSet resultSet) throws SQLException;
  }

  /**
   * Whether the migration user can read one table, logging why not if it cannot.
   */
  private static boolean canRead(Connection connection, ReadCheck readCheck) {
    try (var statement = connection.prepareStatement(readCheck.sql());
         var ignored = statement.executeQuery()) {
      LOGGER.debug("Can read {} in PEARS", readCheck.pearsObject());
      return true;
    } catch (SQLException e) {
      LOGGER.warn("Cannot read {} in PEARS: {}", readCheck.pearsObject(), e.getMessage());
      return false;
    }
  }

  record ReadCheck(String pearsObject, String sql) {
  }

  private static LocalDateTime nullableDateTime(String value) {
    return value == null ? null : LocalDateTime.parse(value);
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
