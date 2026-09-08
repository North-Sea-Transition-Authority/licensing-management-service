package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import uk.co.fivium.energyportalapi.generated.types.OrganisationNameHistory;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.NameHistoryEntryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

/**
 * Splits an organisation's name history around the licence position date being viewed, into the names it was known by
 * before that date and the names it took afterwards.
 *
 * <p>Periods are treated as inclusive of their start date and exclusive of their end date, matching the convention
 * used elsewhere for schedule terms and phases. The name held on the position date itself belongs to neither list.</p>
 */
public final class OrganisationNameHistoryUtil {

  private static final Comparator<OrganisationNameHistory> MOST_RECENTLY_ENDED_FIRST =
      Comparator.comparing(OrganisationNameHistory::getEndDate).reversed();

  private static final Comparator<OrganisationNameHistory> EARLIEST_STARTING_FIRST =
      Comparator.comparing(OrganisationNameHistory::getStartDate);

  private OrganisationNameHistoryUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static OrganisationNameHistoryView getNameHistoryView(
      String organisationName,
      List<OrganisationNameHistory> nameHistory,
      LocalDate positionDate
  ) {
    var namedEntries = nameHistory.stream()
        .filter(entry -> StringUtils.isNotBlank(entry.getName()))
        .toList();

    var previousNames = namedEntries.stream()
        .filter(entry -> isBefore(entry, positionDate))
        .sorted(MOST_RECENTLY_ENDED_FIRST)
        .toList();

    var laterNames = namedEntries.stream()
        .filter(entry -> isAfter(entry, positionDate))
        .sorted(EARLIEST_STARTING_FIRST)
        .toList();

    return new OrganisationNameHistoryView(
        organisationName,
        toEntryViews(previousNames, OrganisationNameHistory::getEndDate, "to %s"),
        toEntryViews(laterNames, OrganisationNameHistory::getStartDate, "from %s")
    );
  }

  /**
   * True when the organisation had already stopped using this name by the position date. An entry whose end date falls
   * exactly on the position date is a previous name, because periods end exclusively.
   */
  private static boolean isBefore(OrganisationNameHistory entry, LocalDate positionDate) {
    return entry.getEndDate() != null && !entry.getEndDate().isAfter(positionDate);
  }

  /** True when the organisation had not yet taken this name on the position date. */
  private static boolean isAfter(OrganisationNameHistory entry, LocalDate positionDate) {
    return entry.getStartDate() != null && entry.getStartDate().isAfter(positionDate);
  }

  /**
   * Maps sorted entries to their view, dropping repeats of a name already listed. Entries arrive ordered by proximity
   * to the position date, so the occurrence closest to it is the one kept.
   */
  private static List<NameHistoryEntryView> toEntryViews(
      List<OrganisationNameHistory> entries,
      Function<OrganisationNameHistory, LocalDate> dateExtractor,
      String dateTextFormat
  ) {
    var seenNames = new HashSet<String>();
    var entryViews = new ArrayList<NameHistoryEntryView>();

    for (var entry : entries) {
      if (isNewName(seenNames, entry.getName())) {
        entryViews.add(new NameHistoryEntryView(
            entry.getName(),
            dateTextFormat.formatted(DateUtil.formatLongDate(dateExtractor.apply(entry)))
        ));
      }
    }

    return List.copyOf(entryViews);
  }

  private static boolean isNewName(Set<String> seenNames, String name) {
    return seenNames.add(name.toUpperCase(Locale.ROOT));
  }
}
