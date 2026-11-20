package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;

/**
 * Gives a licence's mapped events their {@link MappedTerm#eventKey}, version by version, oldest first. PEARS has no
 * stable event id, so each event is matched to the previous mapped version's events (plan, "Schedule history"):
 * <ol>
 *   <li>terms by term number, phases by phase, and the single expiry;</li>
 *   <li>everything else by type and offset;</li>
 *   <li>then by type and comments, where the comment is unique for the type in both versions.</li>
 * </ol>
 * A matched event keeps the previous one's key; anything else gets a new key. ({@code RD} and {@code PA} events,
 * which have an {@code EVENT_ID}, are copied by the write step rather than mapped.)
 */
final class EventKeys {

  static final String MATCHED_IN_ORDER =
      "Events that can't be told apart from the previous version's: matched in the order PEARS holds them";

  private static final Set<String> MATCHED_BY_NUMBER = Set.of("T", "P", "E", "RD", "PA");

  private int lastKey;

  @Nullable
  private MappedScheduleVersion previousVersion;

  private List<PearsScheduleEvent> previousEvents = List.of();

  VersionKeys forVersion(VersionContext context, List<PearsScheduleEvent> events) {
    return new VersionKeys(context, events);
  }

  private int newKey() {
    return ++lastKey;
  }

  /**
   * The keys for one version's events.
   */
  final class VersionKeys {

    private final List<PearsScheduleEvent> events;
    private final Map<PearsScheduleEvent, PearsScheduleEvent> previousSources;

    private VersionKeys(VersionContext context, List<PearsScheduleEvent> events) {
      this.events = events;
      this.previousSources = matchSources(context, matchedBySource(events), matchedBySource(previousEvents));
    }

    int term(int termNo) {
      return previous(MappedScheduleVersion::terms)
          .filter(term -> term.termNo() == termNo)
          .map(MappedTerm::eventKey)
          .findFirst()
          .orElseGet(EventKeys.this::newKey);
    }

    int phase(PhaseType phaseType) {
      return previous(MappedScheduleVersion::phases)
          .filter(phase -> phase.phaseType() == phaseType)
          .map(MappedPhase::eventKey)
          .findFirst()
          .orElseGet(EventKeys.this::newKey);
    }

    int expiry() {
      return Optional.ofNullable(previousVersion)
          .map(MappedScheduleVersion::expiry)
          .map(MappedExpiry::eventKey)
          .orElseGet(EventKeys.this::newKey);
    }

    /**
     * The key of an LMS rate from the {@code R} event {@code source}, anchored to the term or phase given: the
     * previous version's LMS rate from the matched {@code R} event in the same term or phase.
     */
    int rate(PearsScheduleEvent source, int termNo, @Nullable PhaseType phaseType) {
      var previousSource = previousSources.get(source);
      return previous(MappedScheduleVersion::rates)
          .filter(rate -> rate.source().equals(previousSource)
              && rate.termNo() == termNo
              && rate.phaseType() == phaseType)
          .map(MappedRate::eventKey)
          .findFirst()
          .orElseGet(EventKeys.this::newKey);
    }

    int otherEvent(PearsScheduleEvent source) {
      var previousSource = previousSources.get(source);
      return previous(MappedScheduleVersion::otherEvents)
          .filter(otherEvent -> otherEvent.source().equals(previousSource))
          .map(MappedOtherEvent::eventKey)
          .findFirst()
          .orElseGet(EventKeys.this::newKey);
    }

    /**
     * The event's PEARS comments, new unless the event with the same key had the same comments in the previous
     * version. Null when there are none.
     */
    @Nullable
    MappedComment comment(int eventKey, @Nullable String pearsComments) {
      var text = StringUtils.trimToNull(pearsComments);
      if (text == null) {
        return null;
      }

      var previousText = Stream.of(
              previous(MappedScheduleVersion::terms).filter(term -> term.eventKey() == eventKey).map(MappedTerm::comment),
              previous(MappedScheduleVersion::phases).filter(phase -> phase.eventKey() == eventKey).map(MappedPhase::comment),
              previous(MappedScheduleVersion::rates).filter(rate -> rate.eventKey() == eventKey).map(MappedRate::comment)
          )
          .flatMap(Function.identity())
          .filter(Objects::nonNull)
          .map(MappedComment::text)
          .findFirst()
          .orElse(null);

      return new MappedComment(text, !text.equals(previousText));
    }

    /**
     * Makes {@code version} the one the next version is matched to.
     */
    void versionMapped(MappedScheduleVersion version) {
      previousVersion = version;
      previousEvents = events;
    }

    private <T> Stream<T> previous(Function<MappedScheduleVersion, List<T>> mappedEvents) {
      return previousVersion == null ? Stream.empty() : mappedEvents.apply(previousVersion).stream();
    }
  }

  private static List<PearsScheduleEvent> matchedBySource(List<PearsScheduleEvent> events) {
    return events.stream().filter(event -> !MATCHED_BY_NUMBER.contains(event.eventMnem())).toList();
  }

  /**
   * Each of the current version's events matched to the previous version's, by type and offset, then by type and a
   * comment unique for the type in both versions.
   */
  private static Map<PearsScheduleEvent, PearsScheduleEvent> matchSources(
      VersionContext context,
      List<PearsScheduleEvent> current,
      List<PearsScheduleEvent> previous
  ) {
    var matches = new HashMap<PearsScheduleEvent, PearsScheduleEvent>();
    var unmatchedCurrent = new ArrayList<>(current);
    var unmatchedPrevious = new ArrayList<>(previous);

    pair(context, matches, unmatchedCurrent, unmatchedPrevious, EventKeys::offsetKey, EventKeys::offsetKey);
    pair(context, matches, unmatchedCurrent, unmatchedPrevious, uniqueCommentKey(current), uniqueCommentKey(previous));

    return matches;
  }

  private static void pair(
      VersionContext context,
      Map<PearsScheduleEvent, PearsScheduleEvent> matches,
      List<PearsScheduleEvent> unmatchedCurrent,
      List<PearsScheduleEvent> unmatchedPrevious,
      Function<PearsScheduleEvent, Optional<SourceKey>> currentKey,
      Function<PearsScheduleEvent, Optional<SourceKey>> previousKey
  ) {
    var previousByKey = groupByKey(unmatchedPrevious, previousKey);

    groupByKey(unmatchedCurrent, currentKey).forEach((key, currentEvents) -> {
      var previousEvents = previousByKey.get(key);
      if (previousEvents == null) {
        return;
      }
      if (currentEvents.size() > 1 || previousEvents.size() > 1) {
        context.notes().note(MATCHED_IN_ORDER);
      }
      for (var i = 0; i < Math.min(currentEvents.size(), previousEvents.size()); i++) {
        matches.put(currentEvents.get(i), previousEvents.get(i));
        unmatchedCurrent.remove(currentEvents.get(i));
        unmatchedPrevious.remove(previousEvents.get(i));
      }
    });
  }

  private static Map<SourceKey, List<PearsScheduleEvent>> groupByKey(
      List<PearsScheduleEvent> events,
      Function<PearsScheduleEvent, Optional<SourceKey>> key
  ) {
    var groups = new LinkedHashMap<SourceKey, List<PearsScheduleEvent>>();
    events.forEach(event -> key.apply(event)
        .ifPresent(sourceKey -> groups.computeIfAbsent(sourceKey, unused -> new ArrayList<>()).add(event)));
    return groups;
  }

  private static Optional<SourceKey> offsetKey(PearsScheduleEvent event) {
    return VersionContext.offset(event).map(offset -> new SourceKey(event.eventMnem(), offset));
  }

  /**
   * The event's type and comments, when no other event of its type in {@code versionEvents} has the same comments.
   */
  private static Function<PearsScheduleEvent, Optional<SourceKey>> uniqueCommentKey(List<PearsScheduleEvent> versionEvents) {
    var counts = versionEvents.stream()
        .map(EventKeys::commentKey)
        .flatMap(Optional::stream)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    return event -> commentKey(event).filter(key -> counts.get(key) == 1);
  }

  private static Optional<SourceKey> commentKey(PearsScheduleEvent event) {
    return Optional.ofNullable(StringUtils.trimToNull(event.comments()))
        .map(comments -> new SourceKey(event.eventMnem(), comments));
  }

  private record SourceKey(String eventMnem, Object value) {
  }
}
