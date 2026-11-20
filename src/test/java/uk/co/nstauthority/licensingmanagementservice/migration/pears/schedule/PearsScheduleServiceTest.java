package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

@ExtendWith(MockitoExtension.class)
class PearsScheduleServiceTest {

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);

  @Mock
  private DataSource dataSource;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement statement;

  private PearsScheduleService pearsScheduleService;

  @BeforeEach
  void setUp() {
    pearsScheduleService = new PearsScheduleService(dataSource);
  }

  @Test
  void licenceSchedule_readsEveryVersionWithItsEventsInOrder() throws Exception {
    // The first event fills every column with a value no other column holds, so reading any column out of order fails.
    givenQuery(sql("licence-schedule.sql"), rows(
        versionRow(101, "2014-11-20T09:15:00", "2015-03-02T14:00:00", "2014-12-01")
            .event(1, "T", "term", "phase", "1", "2", "3", "2018-12-01")
            .rate("recurring", "recurrances", "rate")
            .recurEndDate("recur end date")
            .percentage("percentage")
            .comments("comments")
            .area("subtype", "event id", "area name", "area short name", "area type", "area version"),
        versionRow(101, "2014-11-20T09:15:00", "2015-03-02T14:00:00", "2014-12-01")
            .event(2, "R", null, null, "0", "0", "0", "2014-12-01")
            .rate("YEARLY", null, "7500"),
        versionRow(102, "2015-03-02T14:00:00", null, "2014-12-01")
            .event(1, "BM", null, null, "8", "11", "29", "2023-11-30")
            .comments("Bespoke milestone wording")
    ));

    var schedule = pearsScheduleService.licenceSchedule(P2244);

    var current = new PearsScheduleVersion(
        102, LocalDateTime.of(2015, Month.MARCH, 2, 14, 0), null, LocalDate.of(2014, Month.DECEMBER, 1),
        List.of(new PearsScheduleEvent(1, "BM", null, null, "8", "11", "29", LocalDate.of(2023, Month.NOVEMBER, 30),
            null, null, null, null, null, "Bespoke milestone wording", null, null, null))
    );
    assertThat(schedule).isEqualTo(new PearsSchedule(P2244, List.of(
        new PearsScheduleVersion(
            101,
            LocalDateTime.of(2014, Month.NOVEMBER, 20, 9, 15),
            LocalDateTime.of(2015, Month.MARCH, 2, 14, 0),
            LocalDate.of(2014, Month.DECEMBER, 1),
            List.of(
                new PearsScheduleEvent(1, "T", "term", "phase", "1", "2", "3", LocalDate.of(2018, Month.DECEMBER, 1),
                    "recurring", "recurrances", "recur end date", "rate", "percentage", "comments", "subtype", "event id",
                    new PearsScheduleEvent.Area("area name", "area short name", "area type", "area version")),
                new PearsScheduleEvent(2, "R", null, null, "0", "0", "0", LocalDate.of(2014, Month.DECEMBER, 1),
                    "YEARLY", null, null, "7500", null, null, null, null, null)
            )
        ),
        current
    )));
    assertThat(schedule.currentVersion()).contains(current);

    verify(statement).setString(1, "P");
    verify(statement).setInt(2, 2244);
  }

  @Test
  void licenceSchedule_whenAVersionHoldsNoEvents_thenItIsStillReturned() throws Exception {
    givenQuery(sql("licence-schedule.sql"), rows(
        versionRow(201, "2004-12-01T10:00:00", null, null)
    ));

    var schedule = pearsScheduleService.licenceSchedule(P2244);

    assertThat(schedule.versions()).singleElement().satisfies(version -> {
      assertThat(version.plsdId()).isEqualTo(201L);
      assertThat(version.originDate()).isNull();
      assertThat(version.events()).isEmpty();
    });
  }

  @Test
  void licenceSchedule_leavesOutPaddingRowsButKeepsEachEventsPosition() throws Exception {
    givenQuery(sql("licence-schedule.sql"), rows(
        versionRow(301, "2019-08-30T08:00:00", null, "2019-08-30").event(1, "T", "2", null, "7", "6", "2", "2027-03-01"),
        versionRow(301, "2019-08-30T08:00:00", null, "2019-08-30").dummyEvent(2),
        versionRow(301, "2019-08-30T08:00:00", null, "2019-08-30").event(3, "MR", null, null, "7", "6", "2", "2027-03-01")
            .percentage("50%")
    ));

    var events = pearsScheduleService.licenceSchedule(P2244).versions().getFirst().events();

    assertThat(events).extracting(PearsScheduleEvent::index).containsExactly(1, 3);
    assertThat(events.get(1).percentage()).isEqualTo("50%");
  }

  @Test
  void licenceSchedule_whenAVersionHoldsOnlyPaddingRows_thenItIsReturnedWithNoEvents() throws Exception {
    givenQuery(sql("licence-schedule.sql"), rows(
        versionRow(401, "2010-01-01T08:00:00", null, "2010-01-01").dummyEvent(1)
    ));

    var schedule = pearsScheduleService.licenceSchedule(P2244);

    assertThat(schedule.versions()).singleElement()
        .satisfies(version -> assertThat(version.events()).isEmpty());
  }

  @Test
  void licenceSchedule_readsAnAreaEventsArea() throws Exception {
    givenQuery(sql("licence-schedule.sql"), rows(
        versionRow(501, "2020-01-01T08:00:00", null, "2008-07-01").event(4, "RD", null, null, null, null, null, "2020-01-01")
            .area("ACTIVE", "1234", "Area name", "A1", "RETENTION", "2")
    ));

    var event = pearsScheduleService.licenceSchedule(P2244).versions().getFirst().events().getFirst();

    assertThat(event.eventSubtype()).isEqualTo("ACTIVE");
    assertThat(event.eventId()).isEqualTo("1234");
    assertThat(event.area())
        .isEqualTo(new PearsScheduleEvent.Area("Area name", "A1", "RETENTION", "2"));
  }

  @Test
  void licenceSchedule_whenPearsHoldsNoSchedule_thenThereAreNoVersions() throws Exception {
    givenQuery(sql("licence-schedule.sql"), rows());

    var schedule = pearsScheduleService.licenceSchedule(P2244);

    assertThat(schedule.versions()).isEmpty();
    assertThat(schedule.currentVersion()).isEmpty();
  }

  @Test
  void licenceSchedule_whenPearsCannotBeRead_thenThrowsNamingTheLicence() throws Exception {
    when(dataSource.getConnection()).thenThrow(new SQLException("ORA-00942: table or view does not exist"));

    assertThatThrownBy(() -> pearsScheduleService.licenceSchedule(P2244))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Could not read the schedule of P2244")
        .hasCauseInstanceOf(SQLException.class);
  }

  @Test
  void scheduleLicenceReferences_leavesOutLicenceTypesPearsDoesNotManageInLms() throws Exception {
    givenQuery(sql("schedule-licence-references.sql"), rows(
        new Object[] {"A", 1},
        new Object[] {"AL", 10},
        new Object[] {"CS", 5},
        new Object[] {"P", 2244},
        new Object[] {"PEDL", 141},
        new Object[] {"XL", 2}
    ));

    var references = pearsScheduleService.scheduleLicenceReferences();

    assertThat(references).containsExactly(
        new PearsLicenceReference("AL", 10),
        new PearsLicenceReference("P", 2244),
        new PearsLicenceReference("PEDL", 141)
    );
  }

  @Test
  void eventTypes_readsTheCatalogue() throws Exception {
    givenQuery(sql("schedule-event-types.sql"), rows(
        new Object[] {"T", "Term", -3},
        new Object[] {"DD", "Drill-or-drop", 4},
        new Object[] {"XX", null, null}
    ));

    assertThat(pearsScheduleService.eventTypes()).containsExactly(
        new PearsScheduleEventType("T", "Term", -3),
        new PearsScheduleEventType("DD", "Drill-or-drop", 4),
        new PearsScheduleEventType("XX", null, null)
    );
  }

  @Test
  void checkReadable_whenEveryTableCanBeRead_thenPasses() throws Exception {
    var noRows = rows();
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(startsWith("SELECT 1 FROM "))).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(noRows);

    pearsScheduleService.checkReadable();

    for (var readCheck : PearsScheduleService.READ_CHECKS) {
      verify(connection).prepareStatement(readCheck.sql());
    }
  }

  @Test
  void checkReadable_whenATableCannotBeRead_thenThrowsNamingIt() throws Exception {
    var unreadable = mock(PreparedStatement.class);
    var noRows = rows();
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(startsWith("SELECT 1 FROM "))).thenReturn(statement);
    when(connection.prepareStatement("SELECT 1 FROM pedmgr.ped_template_event_types WHERE 1 = 0"))
        .thenReturn(unreadable);
    when(statement.executeQuery()).thenReturn(noRows);
    when(unreadable.executeQuery()).thenThrow(new SQLException("ORA-00942: table or view does not exist"));

    assertThatThrownBy(() -> pearsScheduleService.checkReadable())
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessage("The PEARS migration user cannot read pedmgr.ped_template_event_types");
  }

  @Test
  void checkReadable_whenPearsCannotBeConnectedTo_thenThrows() throws Exception {
    when(dataSource.getConnection()).thenThrow(new SQLException("IO Error: The Network Adapter could not establish"));

    assertThatThrownBy(() -> pearsScheduleService.checkReadable())
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessageStartingWith("Could not connect to PEARS");
  }

  @Test
  void licenceScheduleSql_bindsOnlyTheLicence() throws Exception {
    assertThat(sql("licence-schedule.sql").chars().filter(character -> character == '?').count()).isEqualTo(2);
  }

  private void givenQuery(String sql, ResultSet resultSet) throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(sql)).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
  }

  /**
   * A result set over the given rows, each an array of column values in query order.
   */
  private static ResultSet rows(Object... rows) throws SQLException {
    var resultSet = mock(ResultSet.class);
    var cursor = new AtomicInteger(-1);
    var lastWasNull = new AtomicBoolean();

    lenient().when(resultSet.next()).thenAnswer(invocation -> cursor.incrementAndGet() < rows.length);
    lenient().when(resultSet.getString(anyInt())).thenAnswer(invocation -> {
      var value = value(rows, cursor, invocation.getArgument(0), lastWasNull);
      return value == null ? null : value.toString();
    });
    lenient().when(resultSet.getInt(anyInt())).thenAnswer(invocation -> {
      var value = value(rows, cursor, invocation.getArgument(0), lastWasNull);
      return value == null ? 0 : ((Number) value).intValue();
    });
    lenient().when(resultSet.getLong(anyInt())).thenAnswer(invocation -> {
      var value = value(rows, cursor, invocation.getArgument(0), lastWasNull);
      return value == null ? 0L : ((Number) value).longValue();
    });
    lenient().when(resultSet.wasNull()).thenAnswer(invocation -> lastWasNull.get());
    return resultSet;
  }

  private static Object value(Object[] rows, AtomicInteger cursor, int column, AtomicBoolean lastWasNull) {
    var row = rows[cursor.get()];
    var values = row instanceof ScheduleRow scheduleRow ? scheduleRow.values : (Object[]) row;
    var value = values[column - 1];
    lastWasNull.set(value == null);
    return value;
  }

  private static ScheduleRow versionRow(long plsdId, String start, String end, String originDate) {
    return new ScheduleRow(plsdId, start, end, originDate);
  }

  /**
   * One row of {@code licence-schedule.sql}, built up by column.
   */
  private static final class ScheduleRow {

    private final Object[] values = new Object[25];

    private ScheduleRow(long plsdId, String start, String end, String originDate) {
      values[0] = plsdId;
      values[1] = start;
      values[2] = end;
      values[3] = originDate;
    }

    ScheduleRow event(
        int index, String eventMnem, String termNo, String phase, String years, String months, String days,
        String eventDate
    ) {
      values[4] = index;
      values[6] = eventMnem;
      values[7] = termNo;
      values[8] = phase;
      values[9] = years;
      values[10] = months;
      values[11] = days;
      values[12] = eventDate;
      return this;
    }

    ScheduleRow dummyEvent(int index) {
      values[4] = index;
      values[5] = "true";
      return this;
    }

    ScheduleRow rate(String recurring, String recurrances, String rate) {
      values[13] = recurring;
      values[14] = recurrances;
      values[16] = rate;
      return this;
    }

    ScheduleRow recurEndDate(String recurEndDate) {
      values[15] = recurEndDate;
      return this;
    }

    ScheduleRow percentage(String percentage) {
      values[17] = percentage;
      return this;
    }

    ScheduleRow comments(String comments) {
      values[18] = comments;
      return this;
    }

    ScheduleRow area(String subtype, String eventId, String name, String shortName, String type, String version) {
      values[19] = subtype;
      values[20] = eventId;
      values[21] = name;
      values[22] = shortName;
      values[23] = type;
      values[24] = version;
      return this;
    }
  }

  private static String sql(String name) throws IOException {
    try (var inputStream = new ClassPathResource("pears-migration/" + name).getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
