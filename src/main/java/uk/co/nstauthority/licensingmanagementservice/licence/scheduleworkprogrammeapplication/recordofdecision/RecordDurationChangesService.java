package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@Service
public class RecordDurationChangesService {

  static final String MAINTAINED = "Maintained";
  static final String REDUCED_BY = "Reduced by %s";
  static final String EXTENDED_BY = "Extended by %s";

  private final Clock clock;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceScheduleTermRepository licenceScheduleTermRepository;
  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;
  private final RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;
  private final RecordOfDecisionReductionRepository recordOfDecisionReductionRepository;

  public RecordDurationChangesService(
      Clock clock,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceScheduleTermRepository licenceScheduleTermRepository,
      LicenceSchedulePhaseRepository licenceSchedulePhaseRepository,
      RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository,
      RecordOfDecisionReductionRepository recordOfDecisionReductionRepository
  ) {
    this.clock = clock;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
    this.recordOfDecisionExtensionRepository = recordOfDecisionExtensionRepository;
    this.recordOfDecisionReductionRepository = recordOfDecisionReductionRepository;
  }

  private boolean canBeExtended(DurationChangeCandidate candidate, boolean isFinal, LocalDate today) {
    return !hasEnded(candidate, today) && !isFinal;
  }

  private boolean canBeReduced(DurationChangeCandidate candidate, LocalDate today) {
    return !hasEnded(candidate, today) && !isCurrent(candidate, today);
  }

  private boolean hasEnded(DurationChangeCandidate candidate, LocalDate today) {
    return !candidate.endDate().isAfter(today);
  }

  private boolean isCurrent(DurationChangeCandidate candidate, LocalDate today) {
    return !candidate.startDate().isAfter(today) && candidate.endDate().isAfter(today);
  }

  public List<RecordDurationChangeView> getDurationChangeViews(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var today = LocalDate.now(clock);
    var candidates = getScheduleInOrder(applicationDetail);

    var views = new ArrayList<RecordDurationChangeView>();

    for (var index = 0; index < candidates.size(); index++) {
      var candidate = candidates.get(index);
      var isFinal = index == candidates.size() - 1;
      var canExtend = canBeExtended(candidate, isFinal, today);
      var canReduce = canBeReduced(candidate, today);

      views.add(new RecordDurationChangeView(
          candidate.id(),
          candidate.displayName(),
          candidate.isPhase(),
          DateFormatUtil.convertToDisplayText(candidate.endDate()),
          ThreeFieldDurationDisplayUtil.convertToDisplayText(candidate.duration()),
          toThreeFieldDuration(getSpan(candidate)),
          canReduce,
          canExtend,
          hasEnded(candidate, today)));
    }

    return views;
  }

  private List<DurationChangeCandidate> getScheduleInOrder(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var licenceScheduleDetail = scheduleWorkProgrammeApplicationService
        .getCurrentScheduleDetailFromApplicationDetail(applicationDetail);

    var candidates = new ArrayList<DurationChangeCandidate>();

    licenceScheduleTermRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail)
        .stream()
        .sorted(Comparator.comparingInt(term -> term.getTermType().getDisplayOrder()))
        .forEach(term -> {
          var phases = licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term);

          if (phases.isEmpty()) {
            candidates.add(new DurationChangeCandidate(
                term.getId().toString(),
                term.getOriginalEventId(),
                term.getTermType().getDisplayName(),
                term.getStartDate(),
                term.getEndDate(),
                term.getTermDuration(),
                false));
            return;
          }

          phases.stream()
              .sorted(Comparator.comparingInt(phase -> phase.getPhaseType().getDisplayOrder()))
              .forEach(phase -> candidates.add(new DurationChangeCandidate(
                  phase.getId().toString(),
                  phase.getOriginalEventId(),
                  phase.getPhaseType().getDisplayName(),
                  phase.getStartDate(),
                  phase.getEndDate(),
                  phase.getPhaseDuration(),
                  true)));
        });

    return candidates;
  }

  public RecordDurationChangesForm getFilledForm(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var form = new RecordDurationChangesForm();

    var extensionsByEvent = getExtensionsByOriginalEventId(applicationDetail);
    var reductionsByEvent = getReductionsByOriginalEventId(applicationDetail);

    for (var candidate : getScheduleInOrder(applicationDetail)) {
      var id = candidate.id();
      var originalEventId = candidate.originalEventId();
      var reduceInput = RecordDurationChangesForm.newReduceDurationInput(id);
      var extendInput = RecordDurationChangesForm.newExtendDurationInput(id);

      if (extensionsByEvent.containsKey(originalEventId)) {
        form.getChangeType().put(id, DurationChangeType.EXTEND);
        extendInput.setFromThreeFieldDuration(extensionsByEvent.get(originalEventId));
      } else if (reductionsByEvent.containsKey(originalEventId)) {
        form.getChangeType().put(id, DurationChangeType.REDUCE);
        reduceInput.setFromThreeFieldDuration(reductionsByEvent.get(originalEventId));
      } else {
        form.getChangeType().put(id, DurationChangeType.MAINTAIN);
      }

      form.getReduceDuration().put(id, reduceInput);
      form.getExtendDuration().put(id, extendInput);
    }

    return form;
  }

  @Transactional
  public void saveDurationChanges(
      RecordDurationChangesForm form,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var candidatesById = getScheduleInOrder(applicationDetail).stream()
        .collect(Collectors.toMap(DurationChangeCandidate::id, Function.identity()));

    var existingExtensions = recordOfDecisionExtensionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail);
    var existingReductions = recordOfDecisionReductionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail);

    Set<UUID> extendedEventIds = new HashSet<>();
    Set<UUID> reducedEventIds = new HashSet<>();

    for (var entry : form.getChangeType().entrySet()) {
      var candidate = candidatesById.get(entry.getKey());

      if (candidate == null || entry.getValue() == null) {
        continue;
      }

      var input = form.durationFor(candidate.id(), entry.getValue());

      if (input == null) {
        continue;
      }

      var duration = input.toThreeFieldDuration();

      if (entry.getValue() == DurationChangeType.EXTEND) {
        extendedEventIds.add(candidate.originalEventId());
        saveExtension(applicationDetail, candidate, duration, existingExtensions);
      } else if (entry.getValue() == DurationChangeType.REDUCE) {
        reducedEventIds.add(candidate.originalEventId());
        saveReduction(applicationDetail, candidate, duration, existingReductions);
      }
    }

    existingExtensions.stream()
        .filter(extension -> !extendedEventIds.contains(getOriginalEventId(extension)))
        .forEach(recordOfDecisionExtensionRepository::delete);

    existingReductions.stream()
        .filter(reduction -> !reducedEventIds.contains(getOriginalEventId(reduction)))
        .forEach(recordOfDecisionReductionRepository::delete);
  }

  private void saveExtension(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      DurationChangeCandidate candidate,
      ThreeFieldDuration duration,
      List<RecordOfDecisionExtension> existingExtensions
  ) {
    var extension = existingExtensions.stream()
        .filter(existing -> candidate.originalEventId().equals(getOriginalEventId(existing)))
        .findFirst()
        .orElseGet(RecordOfDecisionExtension::new);

    extension.setExtensionDuration(duration);
    extension.setScheduleWorkProgrammeApplicationDetail(applicationDetail);

    var id = UUID.fromString(candidate.id());

    if (candidate.isPhase()) {
      var phase = licenceSchedulePhaseRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("LicenceSchedulePhase not found for ID: %s".formatted(id)));
      extension.setLicenceSchedulePhase(phase);
      extension.setLicenceScheduleTerm(null);
    } else {
      var term = licenceScheduleTermRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("LicenceScheduleTerm not found for ID: %s".formatted(id)));
      extension.setLicenceScheduleTerm(term);
      extension.setLicenceSchedulePhase(null);
    }

    recordOfDecisionExtensionRepository.save(extension);
  }

  private void saveReduction(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      DurationChangeCandidate candidate,
      ThreeFieldDuration duration,
      List<RecordOfDecisionReduction> existingReductions
  ) {
    var reduction = existingReductions.stream()
        .filter(existing -> candidate.originalEventId().equals(getOriginalEventId(existing)))
        .findFirst()
        .orElseGet(RecordOfDecisionReduction::new);

    reduction.setReductionDuration(duration);
    reduction.setScheduleWorkProgrammeApplicationDetail(applicationDetail);

    var id = UUID.fromString(candidate.id());

    if (candidate.isPhase()) {
      var phase = licenceSchedulePhaseRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("LicenceSchedulePhase not found for ID: %s".formatted(id)));
      reduction.setLicenceSchedulePhase(phase);
      reduction.setLicenceScheduleTerm(null);
    } else {
      var term = licenceScheduleTermRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("LicenceScheduleTerm not found for ID: %s".formatted(id)));
      reduction.setLicenceScheduleTerm(term);
      reduction.setLicenceSchedulePhase(null);
    }

    recordOfDecisionReductionRepository.save(reduction);
  }

  private ThreeFieldDuration getTotalExtension(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return ThreeFieldDuration.total(
        recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
            .stream()
            .map(RecordOfDecisionExtension::getExtensionDuration)
            .toList());
  }

  private ThreeFieldDuration getTotalReduction(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return ThreeFieldDuration.total(
        recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
            .stream()
            .map(RecordOfDecisionReduction::getReductionDuration)
            .toList());
  }

  public List<RecordDurationChangeSummaryView> getSummaryViews(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var extensionsByEvent = getExtensionsByOriginalEventId(applicationDetail);
    var reductionsByEvent = getReductionsByOriginalEventId(applicationDetail);

    var views = new ArrayList<RecordDurationChangeSummaryView>();
    LocalDate nextStartDate = null;

    for (var candidate : getScheduleInOrder(applicationDetail)) {
      if (nextStartDate == null) {
        nextStartDate = candidate.startDate();
      }

      var newStartDate = nextStartDate;
      var newEndDate = newStartDate.plus(getNewSpan(candidate, extensionsByEvent, reductionsByEvent)).minusDays(1);

      views.add(new RecordDurationChangeSummaryView(
          candidate.displayName(),
          candidate.isPhase(),
          ThreeFieldDurationDisplayUtil.convertToDisplayText(candidate.duration()),
          DateFormatUtil.convertToDisplayText(candidate.endDate()),
          getChangeDisplayText(candidate, extensionsByEvent, reductionsByEvent),
          ThreeFieldDurationDisplayUtil.convertDatesToDurationDisplayText(newStartDate, newEndDate),
          DateFormatUtil.convertToDisplayText(newEndDate)));

      nextStartDate = newEndDate.plusDays(1);
    }

    return views;
  }

  public boolean isReductionLongerThanPeriod(ThreeFieldDuration duration, ThreeFieldDuration reduction) {
    var remaining = toPeriod(duration).minus(toPeriod(reduction)).normalized();

    return remaining.toTotalMonths() < 0 || (remaining.toTotalMonths() == 0 && remaining.getDays() < 1);
  }

  private Period getNewSpan(
      DurationChangeCandidate candidate,
      Map<UUID, ThreeFieldDuration> extensionsByEvent,
      Map<UUID, ThreeFieldDuration> reductionsByEvent
  ) {
    var span = getSpan(candidate);

    if (extensionsByEvent.containsKey(candidate.originalEventId())) {
      return span.plus(toPeriod(extensionsByEvent.get(candidate.originalEventId())));
    }

    if (reductionsByEvent.containsKey(candidate.originalEventId())) {
      return span.minus(toPeriod(reductionsByEvent.get(candidate.originalEventId())));
    }

    return span;
  }

  private Period getSpan(DurationChangeCandidate candidate) {
    return Period.between(candidate.startDate(), candidate.endDate().plusDays(1));
  }

  private String getChangeDisplayText(
      DurationChangeCandidate candidate,
      Map<UUID, ThreeFieldDuration> extensionsByEvent,
      Map<UUID, ThreeFieldDuration> reductionsByEvent
  ) {
    if (extensionsByEvent.containsKey(candidate.originalEventId())) {
      return EXTENDED_BY.formatted(
          ThreeFieldDurationDisplayUtil.convertToDisplayText(extensionsByEvent.get(candidate.originalEventId())));
    }

    if (reductionsByEvent.containsKey(candidate.originalEventId())) {
      return REDUCED_BY.formatted(
          ThreeFieldDurationDisplayUtil.convertToDisplayText(reductionsByEvent.get(candidate.originalEventId())));
    }

    return MAINTAINED;
  }

  private ThreeFieldDuration toThreeFieldDuration(Period period) {
    var normalised = period.normalized();

    return new ThreeFieldDuration(normalised.getYears(), normalised.getMonths(), normalised.getDays());
  }

  private Period toPeriod(ThreeFieldDuration duration) {
    return Period.of(duration.years(), duration.months(), duration.days());
  }

  private Map<UUID, ThreeFieldDuration> getExtensionsByOriginalEventId(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    return recordOfDecisionExtensionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .collect(Collectors.toMap(this::getOriginalEventId, RecordOfDecisionExtension::getExtensionDuration));
  }

  private Map<UUID, ThreeFieldDuration> getReductionsByOriginalEventId(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    return recordOfDecisionReductionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .collect(Collectors.toMap(this::getOriginalEventId, RecordOfDecisionReduction::getReductionDuration));
  }

  public boolean isComplete(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var hasExtension = recordOfDecisionExtensionRepository
        .existsByScheduleWorkProgrammeApplicationDetail(applicationDetail);

    return hasExtension && getTotalExtension(applicationDetail).equals(getTotalReduction(applicationDetail));
  }

  private UUID getOriginalEventId(RecordOfDecisionExtension extension) {
    return extension.getLicenceSchedulePhase() != null
        ? extension.getLicenceSchedulePhase().getOriginalEventId()
        : extension.getLicenceScheduleTerm().getOriginalEventId();
  }

  private UUID getOriginalEventId(RecordOfDecisionReduction reduction) {
    return reduction.getLicenceSchedulePhase() != null
        ? reduction.getLicenceSchedulePhase().getOriginalEventId()
        : reduction.getLicenceScheduleTerm().getOriginalEventId();
  }
}
