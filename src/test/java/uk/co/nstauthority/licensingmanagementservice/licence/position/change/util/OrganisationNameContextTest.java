package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods.OrganisationNamePeriod;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.NameHistoryEntryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;

class OrganisationNameContextTest {

  private static final LocalDate POSITION_DATE = LocalDate.of(2010, Month.JUNE, 1);
  private static final String UNKNOWN_NAME = "Unknown";

  private static final OrganisationNameContext NAME_CONTEXT = OrganisationNameContext.from(Map.of(
      1, namePeriods("Shell plc",
          namePeriod("Shell UK Exploration", LocalDate.of(1987, Month.JANUARY, 12), LocalDate.of(1999, Month.MARCH, 3)),
          namePeriod("Shell Expro", LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
          namePeriod("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null)
      ),
      2, namePeriods("Never Renamed Ltd", namePeriod("Never Renamed Ltd", LocalDate.of(1980, Month.JANUARY, 1), null))
  ));

  @Test
  void getNameForDate() {
    var result = NAME_CONTEXT.getNameForDate(1, POSITION_DATE, UNKNOWN_NAME);

    assertThat(result).isEqualTo("Shell Expro");
  }

  @Test
  void getNameForDate_whenNoPeriodCoversTheDate_usesTheCurrentName() {
    var result = NAME_CONTEXT.getNameForDate(1, LocalDate.of(1900, Month.JANUARY, 1), UNKNOWN_NAME);

    assertThat(result).isEqualTo("Shell plc");
  }

  @Test
  void getNameForDate_whenTheOrganisationIsUnknown_usesTheUnknownName() {
    var result = NAME_CONTEXT.getNameForDate(3, POSITION_DATE, UNKNOWN_NAME);

    assertThat(result).isEqualTo(UNKNOWN_NAME);
  }

  @Test
  void getNameForDate_whenThereIsNoOrganisation_usesTheUnknownName() {
    var result = NAME_CONTEXT.getNameForDate(null, POSITION_DATE, UNKNOWN_NAME);

    assertThat(result).isEqualTo(UNKNOWN_NAME);
  }

  @Test
  void getNamesForDate() {
    var result = NAME_CONTEXT.getNamesForDate(POSITION_DATE);

    assertThat(result).isEqualTo(Map.of(1, "Shell Expro", 2, "Never Renamed Ltd"));
  }

  @Test
  void getNamesForDate_whenThereIsNoDate_returnsTheCurrentNames() {
    var result = NAME_CONTEXT.getNamesForDate(null);

    assertThat(result).isEqualTo(Map.of(1, "Shell plc", 2, "Never Renamed Ltd"));
  }

  @Test
  void getNamesForDate_whenAnOrganisationHasNoUsableName_captionsItAsNotAvailable() {
    var nameContext = OrganisationNameContext.from(Map.of(
        1, namePeriods("  ", namePeriod("  ", LocalDate.of(1980, Month.JANUARY, 1), null))
    ));

    var result = nameContext.getNamesForDate(POSITION_DATE);

    assertThat(result).isEqualTo(Map.of(1, "Not available"));
  }

  @Test
  void getNameHistoryForDate() {
    var result = NAME_CONTEXT.getNameHistoryForDate(1, POSITION_DATE);

    assertThat(result).isEqualTo(new OrganisationNameHistoryView(
        "Shell Expro",
        List.of(new NameHistoryEntryView("Shell UK Exploration", "to 3 March 1999")),
        List.of(new NameHistoryEntryView("Shell plc", "from 22 January 2022"))
    ));
  }

  @Test
  void getNameHistoryForDate_whenThereIsNoDate_returnsNoNamesEitherSide() {
    var result = NAME_CONTEXT.getNameHistoryForDate(1, null);

    assertThat(result).isEqualTo(new OrganisationNameHistoryView("Shell plc", List.of(), List.of()));
  }

  @Test
  void getNameHistoryForDate_whenTheOrganisationIsUnknown_isHeadedByNotAvailable() {
    var result = NAME_CONTEXT.getNameHistoryForDate(3, POSITION_DATE);

    assertThat(result).isEqualTo(new OrganisationNameHistoryView("Not available", List.of(), List.of()));
  }

  private static OrganisationNamePeriods namePeriods(String currentName, OrganisationNamePeriod... namePeriods) {
    return new OrganisationNamePeriods(currentName, List.of(namePeriods));
  }

  private static OrganisationNamePeriod namePeriod(String name, LocalDate startDate, LocalDate endDate) {
    return new OrganisationNamePeriod(name, startDate, endDate);
  }
}
