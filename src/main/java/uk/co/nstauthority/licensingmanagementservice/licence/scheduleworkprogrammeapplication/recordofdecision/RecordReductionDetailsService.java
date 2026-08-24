package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@Service
public class RecordReductionDetailsService {

  private final Clock clock;
  private final RecordOfDecisionReductionRepository recordOfDecisionReductionRepository;
  private final RecordExtensionDetailsService recordExtensionDetailsService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;
  private final LicenceScheduleTermRepository licenceScheduleTermRepository;

  public RecordReductionDetailsService(
      Clock clock,
      RecordOfDecisionReductionRepository recordOfDecisionReductionRepository,
      RecordExtensionDetailsService recordExtensionDetailsService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceSchedulePhaseRepository licenceSchedulePhaseRepository,
      LicenceScheduleTermRepository licenceScheduleTermRepository
  ) {
    this.clock = clock;
    this.recordOfDecisionReductionRepository = recordOfDecisionReductionRepository;
    this.recordExtensionDetailsService = recordExtensionDetailsService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
  }

  public boolean isReductionComplete(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var reductions = recordOfDecisionReductionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail);

    return !reductions.isEmpty()
        && ThreeFieldDuration.total(reductions.stream().map(RecordOfDecisionReduction::getReductionDuration).toList())
        .equals(recordExtensionDetailsService.getTotalExtensionDuration(applicationDetail));
  }

  public List<RecordReductionDetailsView> getReductionDetailsViews(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var today = LocalDate.now(clock);

    var reductionMap = recordOfDecisionReductionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .collect(Collectors.toMap(this::getReductionIdString, reduction -> reduction));

    return getScheduleInOrder(applicationDetail)
        .stream()
        .skip(1)
        .filter(candidate -> hasNotEnded(candidate.endDate(), today))
        .map(candidate -> toView(candidate, reductionMap))
        .toList();
  }

  private List<ReductionCandidate> getScheduleInOrder(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var licenceScheduleDetail = scheduleWorkProgrammeApplicationService
        .getScheduleDetailFromApplicationDetail(applicationDetail);

    List<ReductionCandidate> candidates = new ArrayList<>();

    licenceScheduleTermRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail)
        .stream()
        .sorted(Comparator.comparingInt(term -> term.getTermType().getDisplayOrder()))
        .forEach(term -> {
          var phases = licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(term);

          if (phases.isEmpty()) {
            candidates.add(new ReductionCandidate(
                term.getId().toString(),
                term.getTermType().getDisplayName(),
                term.getEndDate(),
                false));
            return;
          }

          phases.stream()
              .sorted(Comparator.comparingInt(phase -> phase.getPhaseType().getDisplayOrder()))
              .forEach(phase -> candidates.add(new ReductionCandidate(
                  phase.getId().toString(),
                  phase.getPhaseType().getDisplayName(),
                  phase.getEndDate(),
                  true)));
        });

    return candidates;
  }

  private RecordReductionDetailsView toView(
      ReductionCandidate candidate,
      Map<String, RecordOfDecisionReduction> reductionMap
  ) {
    var reduction = reductionMap.get(candidate.id());

    return new RecordReductionDetailsView(
        candidate.id(),
        candidate.displayName(),
        DateFormatUtil.convertToDisplayText(candidate.endDate()),
        candidate.isPhase(),
        reduction != null,
        reduction != null ? reduction.getReductionDuration() : null
    );
  }

  public RecordReductionDetailsForm getFilledForm(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var form = new RecordReductionDetailsForm();

    getReductionDetailsViews(applicationDetail).forEach(view -> {
      if (view.isPhase()) {
        form.getSelectedPhase().put(view.id(), view.isSelected());
      } else {
        form.getSelectedTerm().put(view.id(), view.isSelected());
      }
      populateReductionDurationMap(form, view.id(), view.duration());
    });

    return form;
  }

  @Transactional
  public void saveReductionDetails(
      RecordReductionDetailsForm form,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var offeredIsPhaseById = getReductionDetailsViews(applicationDetail).stream()
        .collect(Collectors.toMap(RecordReductionDetailsView::id, RecordReductionDetailsView::isPhase));

    var selectedIds = saveSelectedReductions(form, applicationDetail, offeredIsPhaseById);

    if (selectedIds.isEmpty()) {
      return;
    }

    deleteUnselectedReductions(applicationDetail, selectedIds);
  }

  private Set<String> saveSelectedReductions(
      RecordReductionDetailsForm form,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Map<String, Boolean> offeredIsPhaseById
  ) {
    Set<String> selectedIds = new HashSet<>();

    for (String id : submittedIds(form, offeredIsPhaseById)) {
      selectedIds.add(id);
      saveReduction(
          applicationDetail,
          UUID.fromString(id),
          form.getReductionDuration().get(id).toThreeFieldDuration(),
          offeredIsPhaseById.get(id));
    }

    return selectedIds;
  }

  private List<String> submittedIds(RecordReductionDetailsForm form, Map<String, Boolean> offeredIsPhaseById) {
    var ticked = Stream.concat(selectedEntries(form.getSelectedPhase()), selectedEntries(form.getSelectedTerm()))
        .filter(id -> isOffered(id, form, offeredIsPhaseById))
        .toList();

    if (!ticked.isEmpty()) {
      return ticked;
    }

    return isSingleOption(form) ? offeredSingleId(form, offeredIsPhaseById) : List.of();
  }

  private Stream<String> selectedEntries(Map<String, Boolean> selectedMap) {
    return selectedMap == null
        ? Stream.empty()
        : selectedMap.entrySet().stream().filter(entry -> BooleanUtils.isTrue(entry.getValue())).map(Map.Entry::getKey);
  }

  private boolean isSingleOption(RecordReductionDetailsForm form) {
    return form.getReductionDuration() != null && form.getReductionDuration().size() == 1;
  }

  private List<String> offeredSingleId(RecordReductionDetailsForm form, Map<String, Boolean> offeredIsPhaseById) {
    var id = form.getReductionDuration().keySet().iterator().next();
    return isOffered(id, form, offeredIsPhaseById) ? List.of(id) : List.of();
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
          .orElseThrow(() -> new IllegalArgumentException("LicenceSchedulePhase not found for ID: " + id));
      reduction.setLicenceSchedulePhase(phase);
      reduction.setLicenceScheduleTerm(null);
    } else {
      var term = licenceScheduleTermRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("LicenceScheduleTerm not found for ID: " + id));
      reduction.setLicenceScheduleTerm(term);
      reduction.setLicenceSchedulePhase(null);
    }

    recordOfDecisionReductionRepository.save(reduction);
  }

  private void deleteUnselectedReductions(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Set<String> selectedIds
  ) {
    recordOfDecisionReductionRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .filter(reduction -> !selectedIds.contains(getReductionIdString(reduction)))
        .forEach(recordOfDecisionReductionRepository::delete);
  }

  private boolean isOffered(String id, RecordReductionDetailsForm form, Map<String, Boolean> offeredIsPhaseById) {
    return offeredIsPhaseById.containsKey(id) && form.getReductionDuration().get(id) != null;
  }

  private boolean hasNotEnded(LocalDate endDate, LocalDate today) {
    return !endDate.isBefore(today);
  }

  private String getReductionIdString(RecordOfDecisionReduction reduction) {
    return reduction.getLicenceSchedulePhase() != null
        ? reduction.getLicenceSchedulePhase().getId().toString()
        : reduction.getLicenceScheduleTerm().getId().toString();
  }

  private void populateReductionDurationMap(
      RecordReductionDetailsForm form,
      String id,
      ThreeFieldDuration durationValue
  ) {
    var durationInput = RecordReductionDetailsForm.newDurationInput(id);

    if (durationValue != null) {
      durationInput.setFromThreeFieldDuration(durationValue);
    }

    form.getReductionDuration().put(id, durationInput);
  }
}
