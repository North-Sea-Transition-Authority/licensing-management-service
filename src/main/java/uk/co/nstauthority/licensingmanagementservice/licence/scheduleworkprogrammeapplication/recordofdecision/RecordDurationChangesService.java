package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
          canReduce,
          canExtend));
    }

    return views;
  }

  private List<DurationChangeCandidate> getScheduleInOrder(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var licenceScheduleDetail = scheduleWorkProgrammeApplicationService
        .getScheduleDetailFromApplicationDetail(applicationDetail);

    var candidates = new ArrayList<DurationChangeCandidate>();

    licenceScheduleTermRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail)
        .stream()
        .sorted(Comparator.comparingInt(term -> term.getTermType().getDisplayOrder()))
        .forEach(term -> {
          var phases = licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term);

          if (phases.isEmpty()) {
            candidates.add(new DurationChangeCandidate(
                term.getId().toString(),
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

    var extensionsById = recordOfDecisionExtensionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .collect(Collectors.toMap(this::getExtensionIdString, RecordOfDecisionExtension::getExtensionDuration));

    var reductionsById = recordOfDecisionReductionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .collect(Collectors.toMap(this::getReductionIdString, RecordOfDecisionReduction::getReductionDuration));

    for (var candidate : getScheduleInOrder(applicationDetail)) {
      var id = candidate.id();
      var reduceInput = RecordDurationChangesForm.newReduceDurationInput(id);
      var extendInput = RecordDurationChangesForm.newExtendDurationInput(id);

      if (extensionsById.containsKey(id)) {
        form.getChangeType().put(id, DurationChangeType.EXTEND);
        extendInput.setFromThreeFieldDuration(extensionsById.get(id));
      } else if (reductionsById.containsKey(id)) {
        form.getChangeType().put(id, DurationChangeType.REDUCE);
        reduceInput.setFromThreeFieldDuration(reductionsById.get(id));
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
    var isPhaseById = getScheduleInOrder(applicationDetail).stream()
        .collect(Collectors.toMap(DurationChangeCandidate::id, DurationChangeCandidate::isPhase));

    Set<String> extendedIds = new HashSet<>();
    Set<String> reducedIds = new HashSet<>();

    for (var entry : form.getChangeType().entrySet()) {
      var id = entry.getKey();

      if (!isPhaseById.containsKey(id) || entry.getValue() == null) {
        continue;
      }

      var input = form.durationFor(id, entry.getValue());

      if (input == null) {
        continue;
      }

      var duration = input.toThreeFieldDuration();

      if (entry.getValue() == DurationChangeType.EXTEND) {
        extendedIds.add(id);
        saveExtension(applicationDetail, UUID.fromString(id), duration, isPhaseById.get(id));
      } else if (entry.getValue() == DurationChangeType.REDUCE) {
        reducedIds.add(id);
        saveReduction(applicationDetail, UUID.fromString(id), duration, isPhaseById.get(id));
      }
    }

    deleteUnselected(applicationDetail, extendedIds, reducedIds);
  }

  private void saveExtension(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      UUID id,
      ThreeFieldDuration duration,
      boolean isPhase
  ) {
    Optional<RecordOfDecisionExtension> existingExtension;

    if (isPhase) {
      existingExtension = recordOfDecisionExtensionRepository
          .findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(applicationDetail, id);
    } else {
      existingExtension = recordOfDecisionExtensionRepository
          .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, id);
    }

    var extension = existingExtension.orElseGet(RecordOfDecisionExtension::new);

    extension.setExtensionDuration(duration);
    extension.setScheduleWorkProgrammeApplicationDetail(applicationDetail);

    if (isPhase) {
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
      UUID id,
      ThreeFieldDuration duration,
      boolean isPhase
  ) {
    Optional<RecordOfDecisionReduction> existingReduction;

    if (isPhase) {
      existingReduction = recordOfDecisionReductionRepository
          .findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(applicationDetail, id);
    } else {
      existingReduction = recordOfDecisionReductionRepository
          .findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(applicationDetail, id);
    }

    var reduction = existingReduction.orElseGet(RecordOfDecisionReduction::new);

    reduction.setReductionDuration(duration);
    reduction.setScheduleWorkProgrammeApplicationDetail(applicationDetail);

    if (isPhase) {
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

  private void deleteUnselected(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Set<String> extendedIds,
      Set<String> reducedIds
  ) {
    recordOfDecisionExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .filter(extension -> !extendedIds.contains(getExtensionIdString(extension)))
        .forEach(recordOfDecisionExtensionRepository::delete);

    recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .filter(reduction -> !reducedIds.contains(getReductionIdString(reduction)))
        .forEach(recordOfDecisionReductionRepository::delete);
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

  public boolean isComplete(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var hasExtension = recordOfDecisionExtensionRepository
        .existsByScheduleWorkProgrammeApplicationDetail(applicationDetail);

    return hasExtension && getTotalExtension(applicationDetail).equals(getTotalReduction(applicationDetail));
  }

  private String getExtensionIdString(RecordOfDecisionExtension extension) {
    return extension.getLicenceSchedulePhase() != null
        ? extension.getLicenceSchedulePhase().getId().toString()
        : extension.getLicenceScheduleTerm().getId().toString();
  }

  private String getReductionIdString(RecordOfDecisionReduction reduction) {
    return reduction.getLicenceSchedulePhase() != null
        ? reduction.getLicenceSchedulePhase().getId().toString()
        : reduction.getLicenceScheduleTerm().getId().toString();
  }
}
