package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.fivium.energyportalapi.generated.types.OrganisationNameHistory;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.NameHistoryEntryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;

class OrganisationNameHistoryUtilTest {

  private static final String ORGANISATION_NAME = "Shell U.K. Limited";
  private static final LocalDate POSITION_DATE = LocalDate.of(2010, Month.JUNE, 1);

  @Test
  void getNameHistoryView_splitsNamesEitherSideOfThePositionDate() {
    var nameHistory = List.of(
        nameHistory("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null),
        nameHistory(ORGANISATION_NAME, LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
        nameHistory("Shell UK Exploration", LocalDate.of(1987, Month.JANUARY, 12), LocalDate.of(1999, Month.MARCH, 3))
    );

    var result = OrganisationNameHistoryUtil.getNameHistoryView(ORGANISATION_NAME, nameHistory, POSITION_DATE);

    assertThat(result)
        .isEqualTo(new OrganisationNameHistoryView(
            ORGANISATION_NAME,
            List.of(new NameHistoryEntryView("Shell UK Exploration", "to 3 March 1999")),
            List.of(new NameHistoryEntryView("Shell plc", "from 22 January 2022"))
        ));
  }

  @Test
  void getNameHistoryView_whenNameEndsOnThePositionDate_isAPreviousName() {
    var nameHistory = List.of(nameHistory("Old Name Ltd", LocalDate.of(1990, Month.JANUARY, 1), POSITION_DATE));

    var result = OrganisationNameHistoryUtil.getNameHistoryView(ORGANISATION_NAME, nameHistory, POSITION_DATE);

    assertThat(result)
        .isEqualTo(new OrganisationNameHistoryView(
            ORGANISATION_NAME,
            List.of(new NameHistoryEntryView("Old Name Ltd", "to 1 June 2010")),
            List.of()
        ));
  }

  @Test
  void getNameHistoryView_whenNameStartsOnThePositionDate_isExcludedAsTheNameHeldOnTheDate() {
    var nameHistory = List.of(nameHistory("Name On The Day Ltd", POSITION_DATE, null));

    var result = OrganisationNameHistoryUtil.getNameHistoryView(ORGANISATION_NAME, nameHistory, POSITION_DATE);

    assertThat(result)
        .isEqualTo(new OrganisationNameHistoryView(ORGANISATION_NAME, List.of(), List.of()));
  }

  @Test
  void getNameHistoryView_whenNameSpansThePositionDate_isExcludedFromBothLists() {
    var nameHistory = List.of(
        nameHistory(ORGANISATION_NAME, LocalDate.of(2005, Month.JANUARY, 1), LocalDate.of(2015, Month.JANUARY, 1))
    );

    var result = OrganisationNameHistoryUtil.getNameHistoryView(ORGANISATION_NAME, nameHistory, POSITION_DATE);

    assertThat(result)
        .isEqualTo(new OrganisationNameHistoryView(ORGANISATION_NAME, List.of(), List.of()));
  }

  @Test
  void getNameHistoryView_whenDatesAreMissing_skipsTheEntry() {
    var nameHistory = List.of(
        nameHistory("No dates at all Ltd", null, null),
        nameHistory("Never ended Ltd", LocalDate.of(1990, Month.JANUARY, 1), null)
    );

    var result = OrganisationNameHistoryUtil.getNameHistoryView(ORGANISATION_NAME, nameHistory, POSITION_DATE);

    assertThat(result)
        .isEqualTo(new OrganisationNameHistoryView(ORGANISATION_NAME, List.of(), List.of()));
  }

  @Test
  void getNameHistoryView_whenNameIsBlank_skipsTheEntry() {
    var nameHistory = List.of(nameHistory("  ", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(2000, Month.JANUARY, 1)));

    var result = OrganisationNameHistoryUtil.getNameHistoryView(ORGANISATION_NAME, nameHistory, POSITION_DATE);

    assertThat(result)
        .isEqualTo(new OrganisationNameHistoryView(ORGANISATION_NAME, List.of(), List.of()));
  }

  @Test
  void getNameHistoryView_ordersPreviousNamesMostRecentFirstAndLaterNamesSoonestFirst() {
    var nameHistory = List.of(
        nameHistory("Oldest Ltd", LocalDate.of(1970, Month.JANUARY, 1), LocalDate.of(1980, Month.JANUARY, 1)),
        nameHistory("Newest Ltd", LocalDate.of(2030, Month.JANUARY, 1), null),
        nameHistory("Most recent previous Ltd", LocalDate.of(1980, Month.JANUARY, 1), LocalDate.of(1990, Month.JANUARY, 1)),
        nameHistory("Soonest later Ltd", LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2030, Month.JANUARY, 1))
    );

    var result = OrganisationNameHistoryUtil.getNameHistoryView(ORGANISATION_NAME, nameHistory, POSITION_DATE);

    assertThat(result)
        .isEqualTo(new OrganisationNameHistoryView(
            ORGANISATION_NAME,
            List.of(
                new NameHistoryEntryView("Most recent previous Ltd", "to 1 January 1990"),
                new NameHistoryEntryView("Oldest Ltd", "to 1 January 1980")),
            List.of(
                new NameHistoryEntryView("Soonest later Ltd", "from 1 January 2020"),
                new NameHistoryEntryView("Newest Ltd", "from 1 January 2030"))
        ));
  }

  @Test
  void getNameHistoryView_whenTheSameNameIsHeldMoreThanOnce_keepsTheEntryNearestThePositionDate() {
    var nameHistory = List.of(
        nameHistory("Repeated Ltd", LocalDate.of(1970, Month.JANUARY, 1), LocalDate.of(1980, Month.JANUARY, 1)),
        nameHistory("REPEATED LTD", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(2000, Month.JANUARY, 1))
    );

    var result = OrganisationNameHistoryUtil.getNameHistoryView(ORGANISATION_NAME, nameHistory, POSITION_DATE);

    assertThat(result)
        .isEqualTo(new OrganisationNameHistoryView(
            ORGANISATION_NAME,
            List.of(new NameHistoryEntryView("REPEATED LTD", "to 1 January 2000")),
            List.of()
        ));
  }

  @Test
  void getNameHistoryView_whenThereIsNoNameHistory_returnsAViewWithNoNames() {
    var result = OrganisationNameHistoryUtil.getNameHistoryView(ORGANISATION_NAME, List.of(), POSITION_DATE);

    assertThat(result.hasNames()).isFalse();
  }

  private static OrganisationNameHistory nameHistory(String name, LocalDate startDate, LocalDate endDate) {
    return OrganisationNameHistory.newBuilder().name(name).startDate(startDate).endDate(endDate).build();
  }
}
