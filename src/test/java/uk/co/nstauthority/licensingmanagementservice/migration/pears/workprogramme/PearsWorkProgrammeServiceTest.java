package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.inOrder;
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
import java.sql.Statement;
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
class PearsWorkProgrammeServiceTest {

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);

  @Mock
  private DataSource dataSource;

  @Mock
  private Connection connection;

  @Mock
  private Statement transactionStatement;

  private PearsWorkProgrammeService pearsWorkProgrammeService;

  @BeforeEach
  void setUp() {
    pearsWorkProgrammeService = new PearsWorkProgrammeService(dataSource);
  }

  @Test
  void licenceWorkProgrammes_readsEachProgrammeWithItsActivitiesAndNotesInOrder() throws Exception {
    // Each row fills every column with a value no other column holds, so reading any column out of order fails.
    givenLicenceQueries(
        rows(
            new Object[] {11L, 101L, "2019-03-04T10:00:00", "INITIAL", "Initial work programme", "COMPLETE", "4", "0",
                "1"},
            new Object[] {12L, 201L, "2022-06-01T09:30:00", "FALLOW", null, "NOT_STARTED", null, null, null}
        ),
        rows(
            new Object[] {11L, 1, "PHASE", "Phase A", "501", "DRILL_OR_DROP_WELL", "D&D Well 1", "DROPPED", "blocks",
                "One drill or drop well\nto 2,000m", "2021-01-02T03:04:05"},
            new Object[] {11L, 2, "FLAT", null, "502", "SEISMIC", "3D Seismic 1", "COMPLETE", null, null, null},
            new Object[] {12L, 1, "FLAT", null, "601", "OTHER", "Other 1", "NOT_STARTED", null, "Study", null}
        ),
        rows(
            new Object[] {11L, 1, "GENERAL", null, null, null, "2011-07-12T00:00:00", "Migration Process",
                "Start Date: 01/12/2014"},
            new Object[] {11L, 2, "DROPPED", "501", "D&D Well 1", "DROPPED", "2021-01-02T03:04:05", "Jen Brzozowska",
                "Well dropped"}
        )
    );

    var workProgrammes = pearsWorkProgrammeService.licenceWorkProgrammes(P2244);

    assertThat(workProgrammes).containsExactly(
        new PearsWorkProgramme(
            P2244, 11L, 101L, LocalDateTime.of(2019, Month.MARCH, 4, 10, 0),
            "INITIAL", "Initial work programme", "COMPLETE",
            new PearsWorkProgramme.Breakpoint("4", "0", "1"),
            List.of(
                new PearsActivity(1, PearsActivity.ListKind.PHASE, "Phase A", "501", "DRILL_OR_DROP_WELL", "D&D Well 1",
                    "DROPPED", "blocks", "One drill or drop well\nto 2,000m", LocalDateTime.of(2021, Month.JANUARY, 2, 3, 4, 5)),
                new PearsActivity(2, PearsActivity.ListKind.FLAT, null, "502", "SEISMIC", "3D Seismic 1", "COMPLETE", null,
                    null, null)
            ),
            List.of(
                new PearsNote(1, "GENERAL", null, null, null, LocalDateTime.of(2011, Month.JULY, 12, 0, 0),
                    "Migration Process", "Start Date: 01/12/2014"),
                new PearsNote(2, "DROPPED", "501", "D&D Well 1", "DROPPED", LocalDateTime.of(2021, Month.JANUARY, 2, 3, 4, 5),
                    "Jen Brzozowska", "Well dropped")
            )
        ),
        new PearsWorkProgramme(
            P2244, 12L, 201L, LocalDateTime.of(2022, Month.JUNE, 1, 9, 30),
            "FALLOW", null, "NOT_STARTED", null,
            List.of(new PearsActivity(1, PearsActivity.ListKind.FLAT, null, "601", "OTHER", "Other 1", "NOT_STARTED", null,
                "Study", null)),
            List.of()
        )
    );
  }

  @Test
  void licenceWorkProgrammes_readsInOneReadOnlyTransactionAndBindsTheLicence() throws Exception {
    var statements = givenLicenceQueries(rows(), rows(), rows());

    pearsWorkProgrammeService.licenceWorkProgrammes(P2244);

    var order = inOrder(connection, transactionStatement);
    order.verify(connection).setAutoCommit(false);
    order.verify(transactionStatement).execute("SET TRANSACTION READ ONLY");
    order.verify(connection).rollback();
    for (var statement : statements) {
      verify(statement).setString(1, "P");
      verify(statement).setInt(2, 2244);
    }
  }

  @Test
  void licenceWorkProgrammes_whenPearsHoldsNone_thenThereAreNone() throws Exception {
    givenLicenceQueries(rows(), rows(), rows());

    assertThat(pearsWorkProgrammeService.licenceWorkProgrammes(P2244)).isEmpty();
  }

  @Test
  void licenceWorkProgrammes_whenPearsCannotBeRead_thenThrowsNamingTheLicence() throws Exception {
    when(dataSource.getConnection()).thenThrow(new SQLException("ORA-00942: table or view does not exist"));

    assertThatThrownBy(() -> pearsWorkProgrammeService.licenceWorkProgrammes(P2244))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Could not read the work programmes of P2244")
        .hasCauseInstanceOf(SQLException.class);
  }

  @Test
  void workProgrammeLicenceReferences_leavesOutLicenceTypesPearsDoesNotManageInLms() throws Exception {
    var statement = mock(PreparedStatement.class);
    var references = rows(
        new Object[] {"CS", 5},
        new Object[] {"EXL", 253},
        new Object[] {"P", 2244},
        new Object[] {"PEDL", 180},
        new Object[] {"XL", 2}
    );
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(sql("work-programme-licence-references.sql"))).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(references);

    assertThat(pearsWorkProgrammeService.workProgrammeLicenceReferences()).containsExactly(
        new PearsLicenceReference("EXL", 253),
        new PearsLicenceReference("P", 2244),
        new PearsLicenceReference("PEDL", 180)
    );
  }

  @Test
  void checkReadable_whenEveryTableCanBeRead_thenPasses() throws Exception {
    var statement = mock(PreparedStatement.class);
    var noRows = rows();
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(startsWith("SELECT 1 FROM "))).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(noRows);

    pearsWorkProgrammeService.checkReadable();

    for (var readCheck : PearsWorkProgrammeService.READ_CHECKS) {
      verify(connection).prepareStatement(readCheck.sql());
    }
  }

  @Test
  void checkReadable_whenATableCannotBeRead_thenThrowsNamingIt() throws Exception {
    var statement = mock(PreparedStatement.class);
    var unreadable = mock(PreparedStatement.class);
    var noRows = rows();
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(startsWith("SELECT 1 FROM "))).thenReturn(statement);
    when(connection.prepareStatement("SELECT 1 FROM pedmgr.ped_work_programme_details WHERE 1 = 0"))
        .thenReturn(unreadable);
    when(statement.executeQuery()).thenReturn(noRows);
    when(unreadable.executeQuery()).thenThrow(new SQLException("ORA-00942: table or view does not exist"));

    assertThatThrownBy(() -> pearsWorkProgrammeService.checkReadable())
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessage("The PEARS migration user cannot read pedmgr.ped_work_programme_details");
  }

  @Test
  void checkReadable_whenPearsCannotBeConnectedTo_thenThrows() throws Exception {
    when(dataSource.getConnection()).thenThrow(new SQLException("IO Error: The Network Adapter could not establish"));

    assertThatThrownBy(() -> pearsWorkProgrammeService.checkReadable())
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessageStartingWith("Could not connect to PEARS");
  }

  @Test
  void licenceQueries_bindOnlyTheLicence() throws Exception {
    for (var name : List.of("work-programmes.sql", "work-programme-activities.sql", "work-programme-notes.sql")) {
      assertThat(sql(name).chars().filter(character -> character == '?').count()).as(name).isEqualTo(2);
    }
  }

  /**
   * Answers the three per-licence queries with the given rows, returning their statements.
   */
  private List<PreparedStatement> givenLicenceQueries(
      ResultSet workProgrammes,
      ResultSet activities,
      ResultSet notes
  ) throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(transactionStatement);
    var workProgrammesStatement = givenQuery("work-programmes.sql", workProgrammes);
    var activitiesStatement = givenQuery("work-programme-activities.sql", activities);
    var notesStatement = givenQuery("work-programme-notes.sql", notes);
    return List.of(workProgrammesStatement, activitiesStatement, notesStatement);
  }

  private PreparedStatement givenQuery(String name, ResultSet resultSet) throws Exception {
    var statement = mock(PreparedStatement.class);
    when(connection.prepareStatement(sql(name))).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    return statement;
  }

  /**
   * A result set over the given rows, each an array of column values in query order.
   */
  private static ResultSet rows(Object[]... rows) throws SQLException {
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

  private static Object value(Object[][] rows, AtomicInteger cursor, int column, AtomicBoolean lastWasNull) {
    var value = rows[cursor.get()][column - 1];
    lastWasNull.set(value == null);
    return value;
  }

  private static String sql(String name) throws IOException {
    try (var inputStream = new ClassPathResource("pears-migration/" + name).getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
