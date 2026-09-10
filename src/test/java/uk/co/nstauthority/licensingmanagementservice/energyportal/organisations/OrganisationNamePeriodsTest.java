package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.fivium.energyportalapi.generated.types.OrganisationNameHistory;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods.OrganisationNamePeriod;

class OrganisationNamePeriodsTest {

  private static final String ORGANISATION_NAME = "Shell U.K. Limited";
  private static final String CURRENT_NAME = "Known By This Today Ltd";
  private static final LocalDate POSITION_DATE = LocalDate.of(2010, Month.JUNE, 1);

  @Test
  void from() {
    var result = OrganisationNamePeriods.from(CURRENT_NAME, List.of(
        nameHistory("Shell Expro", LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
        nameHistory("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null)
    ));

    assertThat(result).isEqualTo(new OrganisationNamePeriods(CURRENT_NAME, List.of(
        new OrganisationNamePeriod("Shell Expro", LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
        new OrganisationNamePeriod("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null)
    )));
  }

  @Test
  void from_whenThereIsNoNameHistory_keepsTheCurrentName() {
    var result = OrganisationNamePeriods.from(CURRENT_NAME, null);

    assertThat(result).isEqualTo(new OrganisationNamePeriods(CURRENT_NAME, List.of()));
  }

  @Test
  void getNameOnDate_whenAPeriodCoversTheDate_usesThatName() {
    var namePeriods = namePeriods(
        namePeriod("Shell UK Exploration", LocalDate.of(1987, Month.JANUARY, 12), LocalDate.of(1999, Month.MARCH, 3)),
        namePeriod("Shell Expro", LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
        namePeriod("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null)
    );

    var result = namePeriods.getNameOnDate(POSITION_DATE);

    assertThat(result).contains("Shell Expro");
  }

  @Test
  void getNameOnDate_whenTheDateIsAPeriodStartDate_usesThatPeriod() {
    var namePeriods = namePeriods(
        namePeriod("Before Ltd", LocalDate.of(1990, Month.JANUARY, 1), POSITION_DATE),
        namePeriod("Started On The Day Ltd", POSITION_DATE, null)
    );

    var result = namePeriods.getNameOnDate(POSITION_DATE);

    assertThat(result).contains("Started On The Day Ltd");
  }

  @Test
  void getNameOnDate_whenPeriodsCoveringTheDateStartOnDifferentDates_usesTheLatestToStart() {
    var namePeriods = namePeriods(
        namePeriod("Earlier Ltd", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(2015, Month.JANUARY, 1)),
        namePeriod("Later Ltd", LocalDate.of(2005, Month.JANUARY, 1), LocalDate.of(2012, Month.JANUARY, 1))
    );

    var result = namePeriods.getNameOnDate(POSITION_DATE);

    assertThat(result).contains("Later Ltd");
  }

  @Test
  void getNameOnDate_whenPeriodsCoveringTheDateStartTogether_usesTheOneStillInUse() {
    var namePeriods = namePeriods(
        namePeriod("Still In Use Ltd", LocalDate.of(2005, Month.JANUARY, 1), null),
        namePeriod("Superseded Ltd", LocalDate.of(2005, Month.JANUARY, 1), LocalDate.of(2015, Month.JANUARY, 1))
    );

    var result = namePeriods.getNameOnDate(POSITION_DATE);

    assertThat(result).contains("Still In Use Ltd");
  }

  @Test
  void getNameOnDate_whenPeriodsCoveringTheDateCannotBeToldApart_usesTheLastReturned() {
    var namePeriods = namePeriods(
        namePeriod("First Returned Ltd", LocalDate.of(2005, Month.JANUARY, 1), LocalDate.of(2015, Month.JANUARY, 1)),
        namePeriod("Last Returned Ltd", LocalDate.of(2005, Month.JANUARY, 1), LocalDate.of(2015, Month.JANUARY, 1))
    );

    var result = namePeriods.getNameOnDate(POSITION_DATE);

    assertThat(result).contains("Last Returned Ltd");
  }

  @Test
  void getNameOnDate_whenNoPeriodCoversTheDate_usesTheCurrentName() {
    var namePeriods = namePeriods(
        namePeriod("Ended Before Ltd", LocalDate.of(1990, Month.JANUARY, 1), POSITION_DATE),
        namePeriod("Started After Ltd", LocalDate.of(2020, Month.JANUARY, 1), null),
        namePeriod("No start date Ltd", null, null)
    );

    var result = namePeriods.getNameOnDate(POSITION_DATE);

    assertThat(result).contains(CURRENT_NAME);
  }

  @Test
  void getNameOnDate_whenThePeriodCoveringTheDateHasABlankName_usesTheCurrentName() {
    var namePeriods = namePeriods(
        namePeriod("Named Ltd", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(2000, Month.JANUARY, 1)),
        namePeriod("  ", LocalDate.of(2005, Month.JANUARY, 1), null)
    );

    var result = namePeriods.getNameOnDate(POSITION_DATE);

    assertThat(result).contains(CURRENT_NAME);
  }

  @Test
  void getNameOnDate_whenThereIsNoDate_usesTheCurrentName() {
    var namePeriods = namePeriods(
        namePeriod("Shell Expro", LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
        namePeriod("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null)
    );

    var result = namePeriods.getNameOnDate(null);

    assertThat(result).contains(CURRENT_NAME);
  }

  @Test
  void getNameOnDate_whenThereAreNoPeriods_usesTheCurrentName() {
    var result = namePeriods().getNameOnDate(POSITION_DATE);

    assertThat(result).contains(CURRENT_NAME);
  }

  @Test
  void getNameOnDate_whenNoPeriodCoversTheDateAndTheCurrentNameIsBlank_isEmpty() {
    var namePeriods = new OrganisationNamePeriods("  ", List.of(
        namePeriod("Started After Ltd", LocalDate.of(2020, Month.JANUARY, 1), null)
    ));

    var result = namePeriods.getNameOnDate(POSITION_DATE);

    assertThat(result).isEmpty();
  }

  @Test
  void getNameOnDate_whenThereIsNothingKnownAboutTheOrganisation_isEmpty() {
    var result = OrganisationNamePeriods.empty().getNameOnDate(POSITION_DATE);

    assertThat(result).isEmpty();
  }

  @Test
  void getPreviousNamesAndGetLaterNames_splitNamesEitherSideOfTheDate() {
    var shellPlc = namePeriod("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null);
    var shellExpro = namePeriod(ORGANISATION_NAME, LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22));
    var shellUkExploration = namePeriod("Shell UK Exploration", LocalDate.of(1987, Month.JANUARY, 12), LocalDate.of(1999, Month.MARCH, 3));

    var namePeriods = namePeriods(shellPlc, shellExpro, shellUkExploration);

    assertThat(namePeriods.getPreviousNames(POSITION_DATE)).isEqualTo(List.of(shellUkExploration));
    assertThat(namePeriods.getLaterNames(POSITION_DATE)).isEqualTo(List.of(shellPlc));
  }

  @Test
  void getPreviousNames_whenANameEndsOnTheDate_isAPreviousName() {
    var oldName = namePeriod("Old Name Ltd", LocalDate.of(1990, Month.JANUARY, 1), POSITION_DATE);

    var namePeriods = namePeriods(oldName, namePeriod(ORGANISATION_NAME, POSITION_DATE, null));

    assertThat(namePeriods.getPreviousNames(POSITION_DATE)).isEqualTo(List.of(oldName));
  }

  @Test
  void getPreviousNames_ordersTheMostRecentlyHeldFirst() {
    var mostRecent = namePeriod("Most recent previous Ltd", LocalDate.of(1980, Month.JANUARY, 1), LocalDate.of(1990, Month.JANUARY, 1));
    var oldest = namePeriod("Oldest Ltd", LocalDate.of(1970, Month.JANUARY, 1), LocalDate.of(1980, Month.JANUARY, 1));

    var namePeriods = namePeriods(oldest, mostRecent, namePeriod(ORGANISATION_NAME, LocalDate.of(1990, Month.JANUARY, 1), null));

    assertThat(namePeriods.getPreviousNames(POSITION_DATE)).isEqualTo(List.of(mostRecent, oldest));
  }

  @Test
  void getLaterNames_ordersTheSoonestTakenFirst() {
    var soonest = namePeriod("Soonest later Ltd", LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2030, Month.JANUARY, 1));
    var newest = namePeriod("Newest Ltd", LocalDate.of(2030, Month.JANUARY, 1), null);

    var namePeriods = namePeriods(newest, soonest, namePeriod(ORGANISATION_NAME, LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 1)));

    assertThat(namePeriods.getLaterNames(POSITION_DATE)).isEqualTo(List.of(soonest, newest));
  }

  @Test
  void getPreviousNames_whenTheSameNameIsHeldMoreThanOnce_keepsThePeriodNearestTheDate() {
    var nearest = namePeriod("REPEATED LTD", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(2000, Month.JANUARY, 1));

    var namePeriods = namePeriods(
        namePeriod("Repeated Ltd", LocalDate.of(1970, Month.JANUARY, 1), LocalDate.of(1980, Month.JANUARY, 1)),
        nearest,
        namePeriod(ORGANISATION_NAME, LocalDate.of(2000, Month.JANUARY, 1), null)
    );

    assertThat(namePeriods.getPreviousNames(POSITION_DATE)).isEqualTo(List.of(nearest));
  }

  @Test
  void getLaterNames_whenTheSameNameIsHeldMoreThanOnce_keepsThePeriodNearestTheDate() {
    var nearest = namePeriod("Repeated Ltd", LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2030, Month.JANUARY, 1));

    var namePeriods = namePeriods(
        namePeriod("REPEATED LTD", LocalDate.of(2040, Month.JANUARY, 1), null),
        nearest,
        namePeriod(ORGANISATION_NAME, LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 1))
    );

    assertThat(namePeriods.getLaterNames(POSITION_DATE)).isEqualTo(List.of(nearest));
  }

  @Test
  void getPreviousNamesAndGetLaterNames_whenANameSpansTheDate_excludeItFromBoth() {
    var namePeriods = namePeriods(
        namePeriod(ORGANISATION_NAME, LocalDate.of(2005, Month.JANUARY, 1), LocalDate.of(2015, Month.JANUARY, 1))
    );

    assertThat(namePeriods.getPreviousNames(POSITION_DATE)).isEmpty();
    assertThat(namePeriods.getLaterNames(POSITION_DATE)).isEmpty();
  }

  @Test
  void getPreviousNamesAndGetLaterNames_whenDatesAreMissing_skipThePeriod() {
    var namePeriods = namePeriods(namePeriod("No dates at all Ltd", null, null));

    assertThat(namePeriods.getPreviousNames(POSITION_DATE)).isEmpty();
    assertThat(namePeriods.getLaterNames(POSITION_DATE)).isEmpty();
  }

  @Test
  void getPreviousNamesAndGetLaterNames_whenANameIsBlank_skipThePeriod() {
    var namePeriods = namePeriods(
        namePeriod("  ", LocalDate.of(1995, Month.JANUARY, 1), LocalDate.of(2000, Month.JANUARY, 1)),
        namePeriod("  ", LocalDate.of(2020, Month.JANUARY, 1), null)
    );

    assertThat(namePeriods.getPreviousNames(POSITION_DATE)).isEmpty();
    assertThat(namePeriods.getLaterNames(POSITION_DATE)).isEmpty();
  }

  private static OrganisationNamePeriods namePeriods(OrganisationNamePeriod... namePeriods) {
    return new OrganisationNamePeriods(CURRENT_NAME, List.of(namePeriods));
  }

  private static OrganisationNamePeriod namePeriod(String name, LocalDate startDate, LocalDate endDate) {
    return new OrganisationNamePeriod(name, startDate, endDate);
  }

  private static OrganisationNameHistory nameHistory(String name, LocalDate startDate, LocalDate endDate) {
    return OrganisationNameHistory.newBuilder().name(name).startDate(startDate).endDate(endDate).build();
  }
}
