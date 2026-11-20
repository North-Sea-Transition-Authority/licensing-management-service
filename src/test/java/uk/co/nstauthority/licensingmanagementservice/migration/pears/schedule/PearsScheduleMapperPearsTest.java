package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * Checks the mapper's port of PEARS date arithmetic against Oracle itself, and maps the plan's example licences from a
 * real PEARS database. Run by {@code pearsMigrationTest}, as {@link PearsScheduleServicePearsTest} is.
 */
@Tag("pears-migration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
    named = "PEARS_DATASOURCE_URL",
    matches = ".+",
    disabledReason = "No PEARS datasource: set PEARS_DATASOURCE_URL, PEARS_DATASOURCE_USERNAME and PEARS_DATASOURCE_PASSWORD"
)
class PearsScheduleMapperPearsTest {

  /**
   * {@code ADD_MONTHS} over every day from December 2015 to March 2017 (two February ends, one a leap year) and every
   * month count from -13 to 26.
   */
  private static final String ADD_MONTHS_SQL = """
      SELECT TO_CHAR(d.day, 'YYYY-MM-DD'), m.months, TO_CHAR(ADD_MONTHS(d.day, m.months), 'YYYY-MM-DD')
      FROM (SELECT DATE '2015-11-30' + LEVEL day FROM dual CONNECT BY LEVEL <= 487) d
      CROSS JOIN (SELECT LEVEL - 14 months FROM dual CONNECT BY LEVEL <= 40) m
      """;

  private HikariDataSource dataSource;

  private PearsScheduleService pearsScheduleService;

  private List<PearsScheduleEventType> eventTypes;

  private final PearsScheduleMapper mapper = new PearsScheduleMapper();

  @BeforeAll
  void connect() {
    dataSource = PearsTestDataSource.connect();
    pearsScheduleService = new PearsScheduleService(dataSource);
    eventTypes = pearsScheduleService.eventTypes();
  }

  @AfterAll
  void disconnect() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @Test
  void addMonths_givesTheSameDatesAsOracle() throws SQLException {
    var rows = 0;
    var mismatches = new ArrayList<String>();

    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(ADD_MONTHS_SQL);
         var resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        rows++;
        var date = LocalDate.parse(resultSet.getString(1));
        var months = resultSet.getInt(2);
        var oracleDate = LocalDate.parse(resultSet.getString(3));
        var javaDate = PearsDates.addMonths(date, months);

        if (!javaDate.equals(oracleDate)) {
          mismatches.add("%s %+d months: Oracle %s, Java %s".formatted(date, months, oracleDate, javaDate));
        }
      }
    }

    assertThat(rows).isEqualTo(487 * 40);
    assertThat(mismatches).isEmpty();
  }

  @Test
  void map_p2244_hasTheTermsThePlanDescribes() {
    var schedule = pearsScheduleService.licenceSchedule(new PearsLicenceReference("P", 2244));

    var version = mapper.map(schedule, eventTypes).activeVersion();

    assertThat(version.startDate()).isEqualTo(LocalDate.of(2014, Month.DECEMBER, 1));
    assertThat(version.terms())
        .extracting(MappedTerm::termNo, MappedTerm::startDate, MappedTerm::endDate)
        .containsExactly(
            tuple(1, LocalDate.of(2014, Month.DECEMBER, 1), LocalDate.of(2018, Month.NOVEMBER, 30)),
            tuple(2, LocalDate.of(2018, Month.DECEMBER, 1), LocalDate.of(2028, Month.NOVEMBER, 30)),
            tuple(3, LocalDate.of(2028, Month.DECEMBER, 1), LocalDate.of(2040, Month.NOVEMBER, 30))
        );
    assertThat(version.expiry()).isNotNull();
    assertThat(version.expiry().pearsDate()).isEqualTo(LocalDate.of(2040, Month.DECEMBER, 1));
  }

  @Test
  void map_p2522_hasThePhasesThePlanDescribes() {
    var schedule = pearsScheduleService.licenceSchedule(new PearsLicenceReference("P", 2522));

    var version = mapper.map(schedule, eventTypes).activeVersion();

    assertThat(version.phases())
        .extracting(MappedPhase::phaseType, MappedPhase::startDate, MappedPhase::endDate)
        .containsExactly(
            tuple(PhaseType.PHASE_A, LocalDate.of(2020, Month.DECEMBER, 1), LocalDate.of(2023, Month.NOVEMBER, 30)),
            tuple(PhaseType.PHASE_C, LocalDate.of(2023, Month.DECEMBER, 1), LocalDate.of(2024, Month.NOVEMBER, 30))
        );
  }
}
