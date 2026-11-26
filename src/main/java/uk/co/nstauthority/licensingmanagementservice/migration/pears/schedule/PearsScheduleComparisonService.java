package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceComparisons;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * Compares chosen licences' PEARS schedules with their LMS schedules, for checking the migration before and after it
 * runs (plan, design section 6). Reads PEARS as the migration does and LMS as stored, and changes nothing.
 */
@Service
@Profile("migration")
@ConditionalOnPearsDataSource
public class PearsScheduleComparisonService {

  static final String NO_PEARS_SCHEDULE = "PEARS holds no schedule for the licence";
  static final String NO_LMS_SCHEDULE = "No schedule in LMS";
  static final String NO_LMS_VERSION = "No LMS schedule detail from this version";
  static final String NO_PEARS_VERSION = "Not from a PEARS version: added in LMS";
  static final String LEFT_OUT = "Left out of the migration: %s";
  static final String FROM_ANOTHER_VERSION =
      "The LMS detail was migrated from PEARS version %d: PEARS has saved this version since";

  private final PearsScheduleService pearsScheduleService;
  private final PearsScheduleMapper pearsScheduleMapper;
  private final LicenceService licenceService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LmsScheduleVersionReader lmsScheduleVersionReader;
  private final PearsScheduleMigratedLicenceRepository migratedLicenceRepository;
  private final EntityManager entityManager;
  private final PearsLicenceComparisons licenceComparisons;

  PearsScheduleComparisonService(
      PearsScheduleService pearsScheduleService,
      LicenceService licenceService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LmsScheduleVersionReader lmsScheduleVersionReader,
      PearsScheduleMigratedLicenceRepository migratedLicenceRepository,
      EntityManager entityManager,
      PearsLicenceComparisons licenceComparisons
  ) {
    this.pearsScheduleService = pearsScheduleService;
    this.pearsScheduleMapper = new PearsScheduleMapper();
    this.licenceService = licenceService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.lmsScheduleVersionReader = lmsScheduleVersionReader;
    this.migratedLicenceRepository = migratedLicenceRepository;
    this.entityManager = entityManager;
    this.licenceComparisons = licenceComparisons;
  }

  /**
   * The licences the migration wrote a schedule for, in licence order.
   */
  public List<PearsLicenceReference> migratedLicences() {
    return migratedLicenceRepository.findAllLicenceReferences().stream()
        .map(PearsLicenceReference::parse)
        .toList();
  }

  /**
   * One comparison per licence, in the order given. Each licence's LMS rows are let go of once it is compared, so
   * comparing every migrated licence doesn't hold them all.
   *
   * @param allVersions every version, oldest first, rather than PEARS's current version against LMS's active detail
   */
  @Transactional(readOnly = true)
  public List<PearsScheduleComparison> compare(List<PearsLicenceReference> licences, boolean allVersions) {
    var eventTypes = pearsScheduleService.eventTypes();
    var comparisons = new ArrayList<PearsScheduleComparison>();
    for (var licence : licences) {
      comparisons.add(compare(licence, eventTypes, allVersions));
      entityManager.clear();
    }
    return comparisons;
  }

  /**
   * As {@link #compare(List, boolean)}, but several licences at a time, each in a transaction of its own, so it only
   * sees what is committed: for the report, not for a caller comparing what its own transaction wrote.
   */
  public List<PearsScheduleComparison> compareInParallel(List<PearsLicenceReference> licences, boolean allVersions) {
    var eventTypes = pearsScheduleService.eventTypes();
    return licenceComparisons.compare(licences, licence -> compare(licence, eventTypes, allVersions));
  }

  PearsScheduleComparison compare(
      PearsLicenceReference reference,
      List<PearsScheduleEventType> eventTypes,
      boolean allVersions
  ) {
    var licenceReference = reference.licenceReference();
    var licence = licenceService.findLicenceByReference(licenceReference);
    if (licence.isEmpty()) {
      return PearsScheduleComparison.problem(licenceReference, PearsScheduleMigrationService.NOT_IN_LMS);
    }

    var pearsSchedule = pearsScheduleService.licenceSchedule(reference);
    if (pearsSchedule.versions().isEmpty()) {
      return PearsScheduleComparison.problem(licenceReference, NO_PEARS_SCHEDULE);
    }

    MappedSchedule mappedSchedule = null;
    String migrationFailure = null;
    try {
      mappedSchedule = pearsScheduleMapper.map(pearsSchedule, eventTypes);
    } catch (PearsScheduleMappingException e) {
      migrationFailure = e.reason();
    }
    var skipped = mappedSchedule == null ? null : PearsScheduleMigrationService.unsupportedReason(mappedSchedule);
    if (skipped != null) {
      // The migration doesn't write a schedule it skips, so its events aren't missing from LMS but not migrated
      migrationFailure = skipped;
      mappedSchedule = null;
    }

    var lmsVersions = lmsDetails(licence.get(), allVersions).stream()
        .map(lmsScheduleVersionReader::read)
        .toList();
    var pairs = allVersions
        ? pairEveryVersion(pearsSchedule, lmsVersions)
        : pairCurrentVersions(pearsSchedule, lmsVersions);
    var mapping = new Mapping(mappedSchedule);
    var wrongOriginalEvents = allVersions ? wrongOriginalEvents(pairs, mapping) : Set.<UUID>of();
    var noLmsSchedule = lmsVersions.isEmpty();
    var failure = migrationFailure;

    var versions = pairs.stream()
        .map(pair -> version(pair, mapping, wrongOriginalEvents, note(pair, mapping, noLmsSchedule, failure)))
        .toList();
    return new PearsScheduleComparison(licenceReference, null, migrationFailure, versions);
  }

  private static PearsScheduleComparison.Version version(
      Pair pair,
      Mapping mapping,
      Set<UUID> wrongOriginalEvents,
      @Nullable String note
  ) {
    var pearsVersion = pair.pears();
    var lmsVersion = pair.lms();
    var expected = pearsVersion == null ? null : mapping.versions().get(pearsVersion.plsdId());
    return new PearsScheduleComparison.Version(
        pearsVersion == null ? null : pearsVersion.plsdId(),
        pearsVersion == null ? null : pearsVersion.start(),
        pearsVersion == null ? null : pearsVersion.originDate(),
        lmsVersion == null ? null : lmsVersion.detail().getId(),
        lmsVersion == null ? null : lmsVersion.detail().getStatus(),
        lmsVersion == null ? null : lmsVersion.startDate(),
        note,
        PearsScheduleVersionComparer.compare(pearsVersion, lmsVersion, expected, wrongOriginalEvents)
    );
  }

  /**
   * PEARS's current version with LMS's active detail, whichever PEARS version that came from.
   */
  private static List<Pair> pairCurrentVersions(PearsSchedule pearsSchedule, List<LmsScheduleVersion> lmsVersions) {
    var activeDetail = lmsVersions.isEmpty() ? null : lmsVersions.getFirst();
    return List.of(new Pair(activeVersion(pearsSchedule), activeDetail));
  }

  private List<LicenceScheduleDetail> lmsDetails(Licence licence, boolean allVersions) {
    if (!allVersions) {
      return licenceScheduleDetailService
          .getScheduleDetailByLicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE)
          .stream()
          .toList();
    }
    return licenceScheduleDetailService.getAllScheduleDetailsByLicence(licence).stream()
        .sorted(Comparator.comparing(LicenceScheduleDetail::getCreatedInstant,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  /**
   * The version the migration makes active: the current one, or the latest when every version is end-dated.
   */
  private static PearsScheduleVersion activeVersion(PearsSchedule pearsSchedule) {
    return pearsSchedule.versions().stream()
        .filter(PearsScheduleVersion::isCurrent)
        .findFirst()
        .orElse(pearsSchedule.versions().getLast());
  }

  /**
   * Each PEARS version, oldest first, with the LMS detail migrated from it, then any LMS details from no PEARS version.
   */
  private static List<Pair> pairEveryVersion(PearsSchedule pearsSchedule, List<LmsScheduleVersion> lmsVersions) {
    var unpaired = new LinkedHashMap<UUID, LmsScheduleVersion>();
    var byPlsdId = new HashMap<Long, LmsScheduleVersion>();
    lmsVersions.forEach(lmsVersion -> {
      unpaired.put(lmsVersion.detail().getId(), lmsVersion);
      lmsVersion.pearsPlsdId().ifPresent(plsdId -> byPlsdId.putIfAbsent(plsdId, lmsVersion));
    });

    var pairs = new ArrayList<Pair>();
    for (var pearsVersion : pearsSchedule.versions()) {
      var lmsVersion = byPlsdId.get(pearsVersion.plsdId());
      if (lmsVersion != null) {
        unpaired.remove(lmsVersion.detail().getId());
      }
      pairs.add(new Pair(pearsVersion, lmsVersion));
    }
    unpaired.values().forEach(lmsVersion -> pairs.add(new Pair(null, lmsVersion)));
    return pairs;
  }

  @Nullable
  private static String note(Pair pair, Mapping mapping, boolean noLmsSchedule, @Nullable String migrationFailure) {
    if (pair.pears() == null) {
      return NO_PEARS_VERSION;
    }
    if (pair.lms() != null) {
      // Only when comparing the current versions: with every version, each detail is paired by where it came from
      return pair.lms().pearsPlsdId()
          .filter(plsdId -> plsdId != pair.pears().plsdId())
          .map(FROM_ANOTHER_VERSION::formatted)
          .orElse(null);
    }
    var leftOut = mapping.leftOutReasons().get(pair.pears().plsdId());
    if (leftOut != null) {
      return LEFT_OUT.formatted(leftOut);
    }
    if (migrationFailure != null) {
      return null;
    }
    return noLmsSchedule ? NO_LMS_SCHEDULE : NO_LMS_VERSION;
  }

  /**
   * The LMS events whose {@code originalEventId} isn't the id of the first event migrated with the same key, so LMS
   * wouldn't treat them as the same event across versions. Keys come from the mapper, and the first event is the one
   * from the oldest version.
   */
  private static Set<UUID> wrongOriginalEvents(List<Pair> pairs, Mapping mapping) {
    var firstEventIds = new HashMap<Integer, UUID>();
    var wrong = new HashSet<UUID>();

    pairs.stream()
        // Only a version the mapper maps, with the detail migrated from it, has keys to check
        .filter(pair -> pair.pears() != null && pair.lms() != null
            && mapping.versions().containsKey(pair.pears().plsdId()))
        .forEach(pair -> checkOriginalEvents(pair, mapping.versions().get(pair.pears().plsdId()), firstEventIds, wrong));
    return wrong;
  }

  /**
   * Adds to {@code wrong} the pair's events whose {@code originalEventId} isn't the id of the first event with their key.
   *
   * @param firstEventIds the id of the first LMS event with each key so far, added to as keys are first seen
   */
  private static void checkOriginalEvents(
      Pair pair,
      MappedScheduleVersion expected,
      Map<Integer, UUID> firstEventIds,
      Set<UUID> wrong
  ) {
    var expectedEvents = PearsScheduleVersionComparer.ExpectedEvents.of(expected).byIndex();
    PearsScheduleVersionComparer.linkedEvents(pair.lms(), pair.pears().plsdId()).forEach((index, lmsEvents) -> {
      var keys = expectedEvents.getOrDefault(index, List.of());
      for (var i = 0; i < Math.min(keys.size(), lmsEvents.size()); i++) {
        var lmsEvent = lmsEvents.get(i);
        var firstEventId = firstEventIds.computeIfAbsent(keys.get(i).eventKey(), key -> lmsEvent.getId());
        if (!Objects.equals(lmsEvent.getOriginalEventId(), firstEventId)) {
          wrong.add(lmsEvent.getId());
        }
      }
    });
  }

  private record Pair(@Nullable PearsScheduleVersion pears, @Nullable LmsScheduleVersion lms) {
  }

  /**
   * What the mapper makes of the licence, by PEARS version. Empty when the migration fails the licence.
   */
  private record Mapping(Map<Long, MappedScheduleVersion> versions, Map<Long, String> leftOutReasons) {

    Mapping(@Nullable MappedSchedule mappedSchedule) {
      this(
          mappedSchedule == null ? Map.of() : mappedSchedule.versions().stream()
              .collect(Collectors.toMap(MappedScheduleVersion::plsdId, version -> version)),
          mappedSchedule == null ? Map.of() : mappedSchedule.leftOutVersions().stream()
              .collect(Collectors.toMap(MappedSchedule.LeftOutVersion::plsdId, MappedSchedule.LeftOutVersion::reason))
      );
    }
  }
}
