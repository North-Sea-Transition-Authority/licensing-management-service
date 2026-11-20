package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * Reads licence schedules out of the PEARS database, for the schedule migration.
 */
@Service
@ConditionalOnPearsDataSource
public class PearsScheduleService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsScheduleService.class);

  private static final String SCHEDULE_LICENCE_REFERENCES_SQL = sql("schedule-licence-references.sql");
  private static final String LICENCE_SCHEDULE_SQL = sql("licence-schedule.sql");
  private static final String SCHEDULE_EVENT_TYPES_SQL = sql("schedule-event-types.sql");

  /**
   * Everything the queries read, each with a query that reads it and returns nothing. The migration user needs a grant
   * on each, so a run checks them all before it starts rather than failing licence by licence.
   */
  static final List<ReadCheck> READ_CHECKS = List.of(
      new ReadCheck("pedmgr.ped_licence_master", "SELECT 1 FROM pedmgr.ped_licence_master WHERE 1 = 0"),
      new ReadCheck("pedmgr.ped_licence_schedule_details", "SELECT 1 FROM pedmgr.ped_licence_schedule_details WHERE 1 = 0"),
      new ReadCheck("pedmgr.ped_template_event_types", "SELECT 1 FROM pedmgr.ped_template_event_types WHERE 1 = 0")
  );

  private final DataSource dataSource;

  PearsScheduleService(@Qualifier("pearsDataSource") DataSource pearsDataSource) {
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
   * Every licence the schedule migration covers, in licence order: the licences PEARS holds a schedule for whose
   * licence type maps to a type PEARS manages. Any other licence with a schedule is logged and left out, so one
   * licence type LMS does not know cannot fail a whole run.
   */
  public List<PearsLicenceReference> scheduleLicenceReferences() {
    var start = System.nanoTime();
    var references = new ArrayList<PearsLicenceReference>();
    var leftOut = new ArrayList<PearsLicenceReference>();

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(SCHEDULE_LICENCE_REFERENCES_SQL)) {
      statement.setFetchSize(5_000);

      for (var reference : readReferences(statement)) {
        (isPearsManaged(reference.licenceType()) ? references : leftOut).add(reference);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not read the licences with a schedule from PEARS", e);
    }

    if (!leftOut.isEmpty()) {
      LOGGER.warn("Left out {} licences with a PEARS schedule whose licence type PEARS does not manage in LMS: {}",
          leftOut.size(), leftOut);
    }

    LOGGER.info("Found {} licences with a schedule in PEARS in {}ms",
        references.size(), Duration.ofNanos(System.nanoTime() - start).toMillis());
    return references;
  }

  /**
   * One licence's schedule: every version PEARS has saved, oldest first, each with its events.
   *
   * @param licence the licence, for example {@code P2244}
   */
  public PearsSchedule licenceSchedule(PearsLicenceReference licence) {
    var start = System.nanoTime();
    List<PearsScheduleVersion> versions;

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(LICENCE_SCHEDULE_SQL)) {
      statement.setFetchSize(5_000);
      statement.setString(1, licence.licenceType());
      statement.setInt(2, licence.licenceNo());

      versions = readVersions(statement);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not read the schedule of %s".formatted(licence), e);
    }

    LOGGER.info("Read {} schedule versions of {} from PEARS in {}ms",
        versions.size(), licence, Duration.ofNanos(System.nanoTime() - start).toMillis());
    return new PearsSchedule(licence, versions);
  }

  /**
   * The schedule event types PEARS knows, in the order its schedule screen shows them.
   */
  public List<PearsScheduleEventType> eventTypes() {
    var eventTypes = new ArrayList<PearsScheduleEventType>();

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(SCHEDULE_EVENT_TYPES_SQL);
         var resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        eventTypes.add(new PearsScheduleEventType(
            resultSet.getString(1), // mnem
            resultSet.getString(2), // short_title
            nullableInt(resultSet, 3) // display_seq
        ));
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not read the schedule event types from PEARS", e);
    }

    return eventTypes;
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

  private static List<PearsLicenceReference> readReferences(PreparedStatement statement) throws SQLException {
    var references = new ArrayList<PearsLicenceReference>();

    try (var resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        references.add(new PearsLicenceReference(
            resultSet.getString(1), // licence_type
            resultSet.getInt(2) // licence_no
        ));
      }
    }

    return references;
  }

  /**
   * Gathers the query's rows into versions: one row per event, or one with no event for a version holding none, in
   * version order.
   */
  private static List<PearsScheduleVersion> readVersions(PreparedStatement statement) throws SQLException {
    var versions = new LinkedHashMap<Long, VersionRows>();

    try (var resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        var plsdId = resultSet.getLong(1); // plsd_id
        var version = versions.get(plsdId);
        if (version == null) {
          version = new VersionRows(
              plsdId,
              LocalDateTime.parse(resultSet.getString(2)), // start_datetime
              nullableDateTime(resultSet.getString(3)), // end_datetime
              nullableDate(resultSet.getString(4)), // origin_date
              new ArrayList<>()
          );
          versions.put(plsdId, version);
        }

        var eventIndex = nullableInt(resultSet, 5); // event_index
        if (eventIndex != null && !"true".equals(resultSet.getString(6))) { // dummy_event
          version.events().add(readEvent(eventIndex, resultSet));
        }
      }
    }

    return versions.values().stream().map(VersionRows::toVersion).toList();
  }

  private static PearsScheduleEvent readEvent(int eventIndex, ResultSet resultSet) throws SQLException {
    return new PearsScheduleEvent(
        eventIndex,
        resultSet.getString(7), // event_mnem
        resultSet.getString(8), // term_no
        resultSet.getString(9), // phase
        resultSet.getString(10), // years
        resultSet.getString(11), // months
        resultSet.getString(12), // days
        nullableDate(resultSet.getString(13)), // event_date
        resultSet.getString(14), // recurring
        resultSet.getString(15), // recurrances
        resultSet.getString(16), // recur_end_date
        resultSet.getString(17), // rate
        resultSet.getString(18), // percentage
        resultSet.getString(19), // comments
        resultSet.getString(20), // event_subtype
        resultSet.getString(21), // event_id
        PearsScheduleEvent.Area.of(
            resultSet.getString(22), // area_name
            resultSet.getString(23), // area_short_name
            resultSet.getString(24), // area_type
            resultSet.getString(25) // area_version
        )
    );
  }

  /**
   * One table the migration reads, and a fixed query that reads it.
   */
  record ReadCheck(String pearsObject, String sql) {
  }

  /**
   * A version while its rows are being read, its events still being added.
   */
  private record VersionRows(
      long plsdId,
      LocalDateTime start,
      LocalDateTime end,
      LocalDate originDate,
      List<PearsScheduleEvent> events
  ) {

    PearsScheduleVersion toVersion() {
      return new PearsScheduleVersion(plsdId, start, end, originDate, events);
    }
  }

  /**
   * Whether LMS maps the PEARS licence type to a type PEARS manages, which is every type with a PEARS schedule.
   */
  private static boolean isPearsManaged(String licenceType) {
    try {
      return !LicenceType.getFromPrefix(licenceType).isManagedByLms();
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static LocalDate nullableDate(String value) {
    return value == null ? null : LocalDate.parse(value);
  }

  private static LocalDateTime nullableDateTime(String value) {
    return value == null ? null : LocalDateTime.parse(value);
  }

  private static Integer nullableInt(ResultSet resultSet, int column) throws SQLException {
    var value = resultSet.getInt(column);
    return resultSet.wasNull() ? null : value;
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
