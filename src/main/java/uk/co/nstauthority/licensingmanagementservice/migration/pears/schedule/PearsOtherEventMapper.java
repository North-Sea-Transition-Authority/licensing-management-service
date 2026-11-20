package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.MappedOtherEvent.Category;

/**
 * Maps a version's other events to LMS other schedule events, following the plan's mapping table and "Default
 * descriptions":
 * <ul>
 *   <li>{@code MR} is a mandatory relinquishment, with its percentage;</li>
 *   <li>{@code BM} is a bespoke milestone;</li>
 *   <li>{@code DD}, {@code 2M}, {@code 2N} and any user-defined type are an "Other activity", named by the type's
 *       PEARS short title.</li>
 * </ul>
 * The comments become the description, or a default when there are none. Each event is dated relative to the start of
 * the term it falls in, an event on a term's end belonging to the term that ends there ("Which term an event belongs
 * to").
 */
final class PearsOtherEventMapper {

  static final String DEFAULT_DESCRIPTION = "Event with no comments: given a default description";
  static final String PERCENTAGE_PLACEHOLDER = "Mandatory relinquishment percentage 'x': migrated with no percentage";
  static final String PERCENTAGE_NOT_MIGRATED =
      "Mandatory relinquishment percentage that is not 0 to 100 to 1 decimal place: migrated with no percentage";
  static final String EVENT_BEFORE_START = "Event before the schedule start: not migrated";
  static final String EVENT_AFTER_FINAL_TERM =
      "Event after the final term's end: migrated, though an LMS user could not add it";

  /**
   * The types mapped elsewhere: terms, phases, the expiry and rates by the schedule mapper, and the {@code RD} and
   * {@code PA} area events by the write step.
   */
  private static final Set<String> NOT_OTHER_EVENTS = Set.of("T", "P", "E", "R", "RD", "PA");

  private static final String MANDATORY_RELINQUISHMENT = "MR";
  private static final String BESPOKE_MILESTONE = "BM";

  private static final Pattern PERCENTAGE = Pattern.compile("\\d+(\\.\\d+)?|\\.\\d+");
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private PearsOtherEventMapper() {
  }

  /**
   * The version's other events, in the order PEARS holds them.
   *
   * @param shortTitles each PEARS event type's short title, by type
   */
  static List<MappedOtherEvent> otherEvents(
      VersionContext context,
      List<PearsScheduleEvent> events,
      List<MappedTerm> terms,
      Map<String, String> shortTitles,
      EventKeys.VersionKeys keys
  ) {
    var otherEvents = new ArrayList<MappedOtherEvent>();
    var finalTerm = terms.getLast();
    var otherPearsEvents = events.stream()
        .filter(event -> !NOT_OTHER_EVENTS.contains(event.eventMnem()))
        .toList();

    for (var event : otherPearsEvents) {
      var date = context.eventDate(event)
          .orElseThrow(() -> context.cannotMap("A %s event has no date".formatted(event.eventMnem())));

      if (date.isBefore(terms.getFirst().startDate())) {
        context.notes().note(EVENT_BEFORE_START);
        continue;
      }
      if (date.isAfter(finalTerm.endDate())) {
        context.notes().note(EVENT_AFTER_FINAL_TERM);
      }

      var term = terms.stream()
          .filter(candidate -> !date.isAfter(candidate.boundary()))
          .findFirst()
          .orElse(finalTerm);
      var category = category(event.eventMnem());
      var shortTitle = StringUtils.defaultIfBlank(shortTitles.get(event.eventMnem()), event.eventMnem()).trim();
      var comments = StringUtils.trimToNull(event.comments());

      if (comments == null) {
        context.notes().note(DEFAULT_DESCRIPTION);
      }

      otherEvents.add(new MappedOtherEvent(
          keys.otherEvent(event),
          category,
          category == Category.OTHER_ACTIVITY ? shortTitle : null,
          comments != null ? comments : defaultDescription(category, shortTitle),
          comments == null,
          category == Category.MANDATORY_RELINQUISHMENT ? relinquishmentPercentage(context, event.percentage()) : null,
          date,
          term.termNo(),
          LmsScheduleDurations.untilDate(term.startDate(), date),
          event
      ));
    }

    return otherEvents;
  }

  private static Category category(String eventMnem) {
    return switch (eventMnem) {
      case MANDATORY_RELINQUISHMENT -> Category.MANDATORY_RELINQUISHMENT;
      case BESPOKE_MILESTONE -> Category.BESPOKE_MILESTONE;
      default -> Category.OTHER_ACTIVITY;
    };
  }

  private static String defaultDescription(Category category, String shortTitle) {
    return switch (category) {
      case MANDATORY_RELINQUISHMENT -> "Mandatory relinquishment";
      case BESPOKE_MILESTONE -> "Bespoke milestone";
      case OTHER_ACTIVITY -> shortTitle;
    };
  }

  /**
   * The percentage, after trimming and removing a trailing {@code %}. PEARS's placeholder {@code x}, and anything that
   * isn't 0 to 100 to at most 1 decimal place, leave it empty and are noted (plan, "Decision: mandatory relinquishment
   * percentage").
   */
  @Nullable
  private static BigDecimal relinquishmentPercentage(VersionContext context, @Nullable String pearsPercentage) {
    var percentage = StringUtils.trimToNull(StringUtils.removeEnd(StringUtils.trimToEmpty(pearsPercentage), "%"));
    if (percentage == null) {
      return null;
    }
    if ("x".equalsIgnoreCase(percentage)) {
      context.notes().note(PERCENTAGE_PLACEHOLDER);
      return null;
    }

    if (PERCENTAGE.matcher(percentage).matches()) {
      var value = new BigDecimal(percentage);
      if (value.compareTo(ONE_HUNDRED) <= 0 && value.stripTrailingZeros().scale() <= 1) {
        return value;
      }
    }

    context.notes().note(PERCENTAGE_NOT_MIGRATED);
    return null;
  }
}
