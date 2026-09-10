package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods.OrganisationNamePeriod;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.NameHistoryEntryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

/**
 * The names the organisations on a licence timeline have been known by, so a position page can name them as they were
 * on the date it is showing rather than as they are today.
 */
public record OrganisationNameContext(Map<Integer, OrganisationNamePeriods> namePeriodsByOrganisationUnitId) {

  public static OrganisationNameContext from(Map<Integer, OrganisationNamePeriods> namePeriodsByOrganisationUnitId) {
    return new OrganisationNameContext(namePeriodsByOrganisationUnitId);
  }

  /** The name the organisation was known by on the given date, falling back to the given name when unknown. */
  public String getNameForDate(@Nullable Integer organisationUnitId, @Nullable LocalDate date, String unknownName) {
    return namePeriodsFor(organisationUnitId).getNameOnDate(date).orElse(unknownName);
  }

  /** Every organisation's name on the given date, keyed by organisation unit id. */
  public Map<Integer, String> getNamesForDate(@Nullable LocalDate date) {
    return namePeriodsByOrganisationUnitId.keySet().stream()
        .collect(Collectors.toMap(
            Function.identity(),
            organisationUnitId -> getNameForDate(organisationUnitId, date, NOT_AVAILABLE)
        ));
  }

  /**
   * The names the organisation held either side of the given date, headed by the name it was known by on the date
   * itself. Without a date only that name is known, so both lists come back empty.
   */
  public OrganisationNameHistoryView getNameHistoryForDate(Integer organisationUnitId, @Nullable LocalDate date) {
    var nameOnDate = getNameForDate(organisationUnitId, date, NOT_AVAILABLE);

    if (date == null) {
      return new OrganisationNameHistoryView(nameOnDate, List.of(), List.of());
    }

    var namePeriods = namePeriodsFor(organisationUnitId);

    return new OrganisationNameHistoryView(
        nameOnDate,
        toEntryViews(namePeriods.getPreviousNames(date), OrganisationNamePeriod::endDate, "to %s"),
        toEntryViews(namePeriods.getLaterNames(date), OrganisationNamePeriod::startDate, "from %s")
    );
  }

  private OrganisationNamePeriods namePeriodsFor(@Nullable Integer organisationUnitId) {
    if (organisationUnitId == null) {
      return OrganisationNamePeriods.empty();
    }

    return namePeriodsByOrganisationUnitId.getOrDefault(organisationUnitId, OrganisationNamePeriods.empty());
  }

  private static List<NameHistoryEntryView> toEntryViews(
      List<OrganisationNamePeriod> namePeriods,
      Function<OrganisationNamePeriod, LocalDate> dateExtractor,
      String dateTextFormat
  ) {
    return namePeriods.stream()
        .map(namePeriod -> new NameHistoryEntryView(
            namePeriod.name(),
            dateTextFormat.formatted(DateUtil.formatLongDate(dateExtractor.apply(namePeriod)))
        ))
        .toList();
  }
}
