package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.LicenceOperationHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsCompanyListType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsHistoryTestUtil;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;

@ExtendWith(MockitoExtension.class)
class PearsLicenceServiceTest {

  private static final String LICENCE_TYPE = "P";
  private static final int LICENCE_NUMBER = 8;
  private static final LocalDate POSITION_DATE = LocalDate.of(1964, Month.SEPTEMBER, 18);

  @Mock
  private DataSource dataSource;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement statement;

  @Mock
  private ResultSet resultSet;

  private PearsLicenceService pearsLicenceService;

  @BeforeEach
  void setUp() {
    pearsLicenceService = new PearsLicenceService(dataSource);
  }

  @Test
  void licenceHistory_whenPearsHoldsOperations_thenReadsTheHistory() throws Exception {
    var xml = PearsHistoryTestUtil.history(LICENCE_TYPE, LICENCE_NUMBER)
        .position(15188, "XPT/1", POSITION_DATE, 6)
        .administratorSet(12)
        .and()
        .toXml();
    givenQuery(sql("licence-history.sql").formatted("'CONSORTIUM_LIST_CHANGE','CONSORTIUM_LIST_CREATE'"));
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getCharacterStream(1)).thenReturn(new StringReader(xml));

    var history = pearsLicenceService.licenceHistory(
        LICENCE_TYPE,
        LICENCE_NUMBER,
        Set.of("CONSORTIUM_LIST_CREATE", "CONSORTIUM_LIST_CHANGE")
    );

    var expected = new LicenceOperationHistory(LICENCE_TYPE, LICENCE_NUMBER, List.of(
        new PearsOperation.ConsortiumListChange(
            new PearsOperation.Header(
                15188, "XPT/1", POSITION_DATE, 6, 1000, 1, PearsOperation.OperationStatus.LIVE, POSITION_DATE, null
            ),
            null,
            PearsCompanyListType.LICENCE_ADMINISTRATOR,
            List.of(new PearsOperation.ConsortiumEntry(
                PearsOperation.EntryType.SET,
                new PearsOperation.Organisation(12, "Organisation 12"),
                null,
                null
            ))
        )
    ));
    assertThat(history).usingRecursiveComparison().isEqualTo(expected);
    verifyLicenceBoundToEveryPair(6);
  }

  @Test
  void licenceHistory_whenPearsHoldsNoOperations_thenReturnsAnEmptyHistory() throws Exception {
    givenQuery(sql("licence-history.sql").formatted("'CONSORTIUM_LIST_CHANGE'"));
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getCharacterStream(1)).thenReturn(null);

    var history = pearsLicenceService.licenceHistory(LICENCE_TYPE, LICENCE_NUMBER, Set.of("CONSORTIUM_LIST_CHANGE"));

    assertThat(history).isEqualTo(new LicenceOperationHistory(LICENCE_TYPE, LICENCE_NUMBER, List.of()));
  }

  @Test
  void licenceHistory_whenNoOperationTypesAreDeclared_thenNoOperationCarriesItsXml() throws Exception {
    givenQuery(sql("licence-history.sql").formatted("NULL"));
    when(resultSet.next()).thenReturn(false);

    var history = pearsLicenceService.licenceHistory(LICENCE_TYPE, LICENCE_NUMBER, Set.of());

    assertThat(history).isEqualTo(new LicenceOperationHistory(LICENCE_TYPE, LICENCE_NUMBER, List.of()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"consortium_list_change", "X') OR ('1'='1", "", "CONSORTIUM-LIST-CHANGE", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
  void licenceHistory_whenAnOperationTypeIsNotATypeName_thenThrowsWithoutQueryingPears(String operationType) {
    assertThatThrownBy(() -> pearsLicenceService.licenceHistory(LICENCE_TYPE, LICENCE_NUMBER, Set.of(operationType)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not a PEARS operation type: " + operationType);

    verifyNoInteractions(dataSource);
  }

  @Test
  void licenceHistory_whenTheHistoryCannotBeParsed_thenThrows() throws Exception {
    givenQuery(sql("licence-history.sql").formatted("NULL"));
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getCharacterStream(1)).thenReturn(new StringReader("<LICENCE_OPERATION_HISTORY"));

    assertThatThrownBy(() -> pearsLicenceService.licenceHistory(LICENCE_TYPE, LICENCE_NUMBER, Set.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Could not read the operation history for P8")
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void dataPointPositions_whenPearsHoldsDataPoints_thenReconstructsThePositionsInOrder() throws Exception {
    givenQuery(sql("data-point-positions.sql"));
    when(resultSet.next()).thenReturn(true, true, false);
    when(resultSet.getString(1)).thenReturn(LICENCE_TYPE, LICENCE_TYPE);
    when(resultSet.getInt(2)).thenReturn(LICENCE_NUMBER, LICENCE_NUMBER);
    when(resultSet.getString(3)).thenReturn("XPT/2", "XPT/1");
    when(resultSet.getString(4)).thenReturn("1964-09-18", "1964-09-18");
    when(resultSet.getInt(5)).thenReturn(7, 3);
    when(resultSet.getLong(6)).thenReturn(15190L, 15188L);

    var positions = pearsLicenceService.dataPointPositions(LICENCE_TYPE, LICENCE_NUMBER);

    assertThat(positions).isEqualTo(new PearsLicencePositions(LICENCE_TYPE, LICENCE_NUMBER, List.of(
        new PearsLicencePositions.Position(POSITION_DATE, 3, 1, "XPT/1", 15188L),
        new PearsLicencePositions.Position(POSITION_DATE, 7, 2, "XPT/2", 15190L)
    )));
    verifyLicenceBoundToEveryPair(1);
  }

  @Test
  void dataPointPositions_whenPearsHoldsNoDataPoints_thenThereAreNoPositions() throws Exception {
    givenQuery(sql("data-point-positions.sql"));
    when(resultSet.next()).thenReturn(false);

    var positions = pearsLicenceService.dataPointPositions(LICENCE_TYPE, LICENCE_NUMBER);

    assertThat(positions).isEqualTo(new PearsLicencePositions(LICENCE_TYPE, LICENCE_NUMBER, List.of()));
  }

  @Test
  void dataPointStates() throws Exception {
    givenQuery(sql("data-point-states.sql"));
    when(resultSet.next()).thenReturn(true, true, false);
    when(resultSet.getString(1)).thenReturn("1964-09-18", "1964-09-18");
    when(resultSet.getInt(2)).thenReturn(6, 6);
    when(resultSet.getString(3)).thenReturn("LICENSEE", "SUBAREA");
    when(resultSet.getInt(4)).thenReturn(20, 0);
    when(resultSet.getInt(5)).thenReturn(0, 194508);
    when(resultSet.getInt(6)).thenReturn(0, 194542);
    when(resultSet.wasNull()).thenReturn(false, true, true, true, false, false);
    when(resultSet.getString(7)).thenReturn(null, "ALL");

    var states = pearsLicenceService.dataPointStates(LICENCE_TYPE, LICENCE_NUMBER);

    assertThat(states).isEqualTo(Map.of(
        new PearsLicencePositions.PositionKey(POSITION_DATE, 6),
        new PearsPositionState(
            Set.of(),
            Set.of(20),
            Set.of(),
            Set.of(new PearsPositionState.Subarea(194508, 194542, "ALL"))
        )
    ));
    verifyLicenceBoundToEveryPair(3);
  }

  @Test
  void licenceReferences() throws Exception {
    givenQuery(sql("licence-references.sql"));
    when(resultSet.next()).thenReturn(true, true, false);
    when(resultSet.getString(1)).thenReturn("P", "PL");
    when(resultSet.getInt(2)).thenReturn(8, 1);

    var references = pearsLicenceService.licenceReferences();

    assertThat(references).containsExactly(
        new PearsLicenceReference("P", 8),
        new PearsLicenceReference("PL", 1)
    );
  }

  @ParameterizedTest
  @MethodSource("queries")
  void eachQuery_whenPearsCannotBeRead_thenThrowsNamingWhatCouldNotBeRead(
      Consumer<PearsLicenceService> query,
      String expectedMessage
  ) throws Exception {
    var cause = new SQLException("PEARS is unavailable");
    when(dataSource.getConnection()).thenThrow(cause);

    assertThatThrownBy(() -> query.accept(pearsLicenceService))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(expectedMessage)
        .hasCause(cause);
  }

  private static Stream<Arguments> queries() {
    return Stream.of(
        Arguments.of(
            Named.<Consumer<PearsLicenceService>>of("licenceHistory",
                service -> service.licenceHistory(LICENCE_TYPE, LICENCE_NUMBER, Set.of())),
            "Could not read the operation history for P8"
        ),
        Arguments.of(
            Named.<Consumer<PearsLicenceService>>of("dataPointPositions",
                service -> service.dataPointPositions(LICENCE_TYPE, LICENCE_NUMBER)),
            "Could not read data point positions for P8"
        ),
        Arguments.of(
            Named.<Consumer<PearsLicenceService>>of("dataPointStates",
                service -> service.dataPointStates(LICENCE_TYPE, LICENCE_NUMBER)),
            "Could not read data point states for P8"
        ),
        Arguments.of(
            Named.<Consumer<PearsLicenceService>>of("licenceReferences", PearsLicenceService::licenceReferences),
            "Could not read the licences to compare from PEARS"
        )
    );
  }

  private void givenQuery(String sql) throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(sql)).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
  }

  private void verifyLicenceBoundToEveryPair(int pairs) throws SQLException {
    for (var pair = 0; pair < pairs; pair++) {
      verify(statement).setString(2 * pair + 1, LICENCE_TYPE);
      verify(statement).setInt(2 * pair + 2, LICENCE_NUMBER);
    }
    verify(statement, times(pairs)).setString(anyInt(), anyString());
    verify(statement, times(pairs)).setInt(anyInt(), anyInt());
  }

  private static String sql(String name) throws IOException {
    try (var inputStream = new ClassPathResource("pears-migration/" + name).getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
