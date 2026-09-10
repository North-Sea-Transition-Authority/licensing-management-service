package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import uk.co.fivium.energyportalapi.generated.types.OrganisationNameHistory;

/**
 * The name an organisation currently goes by and the periods it was known by each of its names, as the Energy Portal
 * holds them. Periods are inclusive of their start date and exclusive of their end date, matching the convention used
 * elsewhere for schedule terms and phases, so the name held on a date is neither a previous nor a later name.
 */
public record OrganisationNamePeriods(
    @Nullable String currentName,
    List<OrganisationNamePeriod> namePeriods
) {

  private static final Comparator<OrganisationNamePeriod> MOST_RECENTLY_ENDED_FIRST =
      Comparator.comparing(OrganisationNamePeriod::endDate).reversed();

  private static final Comparator<OrganisationNamePeriod> EARLIEST_STARTING_FIRST =
      Comparator.comparing(OrganisationNamePeriod::startDate);

  /** A period that never ended is the later of two sharing a start date, so null end dates sort last. */
  private static final Comparator<OrganisationNamePeriod> BY_PERIOD_DATES =
      Comparator.comparing(OrganisationNamePeriod::startDate)
          .thenComparing(OrganisationNamePeriod::endDate, Comparator.nullsLast(Comparator.naturalOrder()));

  public OrganisationNamePeriods {
    currentName = StringUtils.trimToNull(currentName);
    namePeriods = namePeriods.stream()
        .filter(period -> StringUtils.isNotBlank(period.name()) && period.startDate() != null)
        .toList();
  }

  public static OrganisationNamePeriods empty() {
    return new OrganisationNamePeriods(null, List.of());
  }

  public static OrganisationNamePeriods from(
      @Nullable String currentName,
      @Nullable Collection<OrganisationNameHistory> organisationNameHistory
  ) {
    return new OrganisationNamePeriods(
        currentName,
        CollectionUtils.emptyIfNull(organisationNameHistory).stream().map(OrganisationNamePeriod::from).toList()
    );
  }

  /**
   * The name the organisation held on the given date. If no date is given or the date isn't covered by the name history,
   * the current name is returned. Empty when no usable name is held at all.
   */
  public Optional<String> getNameOnDate(@Nullable LocalDate date) {
    if (date == null) {
      return Optional.ofNullable(currentName);
    }

    return namePeriods.stream()
        .filter(period -> period.covers(date))
        .reduce(OrganisationNamePeriods::later)
        .map(OrganisationNamePeriod::name)
        .or(() -> Optional.ofNullable(currentName));
  }

  /** The names the organisation had already stopped using by the given date, most recently held first. */
  public List<OrganisationNamePeriod> getPreviousNames(LocalDate date) {
    return distinctByName(namePeriods.stream()
        .filter(period -> period.endedBy(date))
        .sorted(MOST_RECENTLY_ENDED_FIRST));
  }

  /** The names the organisation had not yet taken on the given date, earliest first. */
  public List<OrganisationNamePeriod> getLaterNames(LocalDate date) {
    return distinctByName(namePeriods.stream()
        .filter(period -> period.startsAfter(date))
        .sorted(EARLIEST_STARTING_FIRST));
  }

  /** Periods that cannot be told apart by their dates fall back to the order the Energy Portal returned them in. */
  private static OrganisationNamePeriod later(OrganisationNamePeriod current, OrganisationNamePeriod candidate) {
    return BY_PERIOD_DATES.compare(candidate, current) >= 0 ? candidate : current;
  }

  /** Periods arrive ordered by proximity to the date being read against, so the nearest occurrence is the one kept. */
  private static List<OrganisationNamePeriod> distinctByName(Stream<OrganisationNamePeriod> periods) {
    var seenNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    return periods.filter(period -> seenNames.add(period.name())).toList();
  }

  public record OrganisationNamePeriod(
      String name,
      LocalDate startDate,
      LocalDate endDate
  ) {

    public static OrganisationNamePeriod from(OrganisationNameHistory organisationNameHistory) {
      return new OrganisationNamePeriod(
          organisationNameHistory.getName(),
          organisationNameHistory.getStartDate(),
          organisationNameHistory.getEndDate()
      );
    }

    private boolean covers(LocalDate date) {
      return !startDate.isAfter(date) && (endDate == null || endDate.isAfter(date));
    }

    /** A period whose end date falls on the given date has ended, because periods end exclusively. */
    private boolean endedBy(LocalDate date) {
      return endDate != null && !endDate.isAfter(date);
    }

    private boolean startsAfter(LocalDate date) {
      return startDate.isAfter(date);
    }
  }
}
