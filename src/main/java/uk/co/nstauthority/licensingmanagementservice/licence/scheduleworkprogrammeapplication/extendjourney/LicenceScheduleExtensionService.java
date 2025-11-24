package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceScheduleExtensionService {
  private final Clock clock;
  private final LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;
  private final LicenceScheduleTermRepository licenceScheduleTermRepository;

  public LicenceScheduleExtensionService(
      Clock clock,
      LicenceScheduleExtensionRepository licenceScheduleExtensionRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceSchedulePhaseRepository licenceSchedulePhaseRepository,
      LicenceScheduleTermRepository licenceScheduleTermRepository
  ) {

    this.clock = clock;
    this.licenceScheduleExtensionRepository = licenceScheduleExtensionRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
  }

  public LicenceScheduleTerm getCurrentTerm(LicenceScheduleDetail licenceScheduleDetail) {

    var licenceScheduleTerms = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(
        licenceScheduleDetail);

    return licenceScheduleTerms
        .stream()
        .filter(term -> isCurrentlyActive(term.getStartDate(), term.getEndDate()))
        .findFirst()
        .orElse(null);
  }

  public LicenceSchedulePhase getCurrentPhase(LicenceScheduleTerm licenceScheduleTerms) {

    var licenceSchedulePhases = licenceSchedulePhaseService
        .getActivePhasesByTerm(licenceScheduleTerms);

    return licenceSchedulePhases
        .stream()
        .filter(phase -> isCurrentlyActive(phase.getStartDate(), phase.getEndDate()))
        .findFirst()
        .orElse(null);
  }

  public boolean isExtensionRequested(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceScheduleExtensionRepository
        .existsLicenceScheduleExtensionRequestByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail);
  }

  boolean isCurrentlyActive(LocalDate startDate, LocalDate endDate) {

    LocalDate today = LocalDate.now(clock);

    if (startDate == null || endDate == null) {
      throw new IllegalArgumentException("Start date and end date cannot be null");
    }

    boolean hasStarted = startDate.isBefore(today) || startDate.isEqual(today);
    boolean hasNotEnded = endDate.isAfter(today);

    return hasStarted && hasNotEnded;
  }

  public List<LicenceScheduleTermAndPhases> getExtendableTermAndPhases(LicenceScheduleDetail licenceScheduleDetail) {
    var today = LocalDate.now(clock);
    List<LicenceScheduleTerm> currentOrFutureTerms = licenceScheduleTermService
        .getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail);

    return currentOrFutureTerms
        .stream()
        .map(term -> mapTermToExtendableTermAndPhases(term, currentOrFutureTerms, today)) // Extracted to private method
        .filter(Objects::nonNull)
        .toList();
  }

  private LicenceScheduleTermAndPhases mapTermToExtendableTermAndPhases(
      LicenceScheduleTerm term,
      List<LicenceScheduleTerm> currentOrFutureTerms,
      LocalDate today
  ) {
    UUID termId = term.getId();
    boolean isFinalTerm = isFinalTerm(term, currentOrFutureTerms);
    boolean hasPhases = licenceSchedulePhaseRepository.existsByLicenceScheduleTermId(termId);
    boolean isTermWithoutPhases = isActiveTermWithoutPhases(term, today, isFinalTerm, hasPhases);

    var extendablePhases =
        getExtendablePhasesInTerm(term, today, isFinalTerm, hasPhases);


    if (!isTermWithoutPhases && extendablePhases.isEmpty()) {
      return null;
    }

    String termIdStr = isTermWithoutPhases ? termId.toString() : null;

    return new LicenceScheduleTermAndPhases(
        termIdStr,
        term.getTermType().getDisplayName(),
        extendablePhases
    );
  }

  private boolean isActiveTermWithoutPhases(
      LicenceScheduleTerm term,
      LocalDate today,
      boolean isLastTerm,
      boolean hasPhases
  ) {

    if (hasPhases) {
      return false;
    }

    if (isLastTerm) {
      return isActive(term.getStartDate(), term.getEndDate(), today);
    }

    return true;
  }

  private List<LicenceScheduleTermAndPhases.PhaseDetails> getExtendablePhasesInTerm(
      LicenceScheduleTerm term,
      LocalDate today,
      boolean isLastTerm,
      boolean hasPhases
  ) {
    if (!hasPhases) {
      return Collections.emptyList();
    }

    boolean isValidForExtensionRequest = !isLastTerm || isActive(term.getStartDate(), term.getEndDate(), today);

    if (!isValidForExtensionRequest) {
      return Collections.emptyList();
    }

    return licenceSchedulePhaseService.getActivePhasesByTerm(term)
                                      .stream()
                                      .filter(phase -> hasNotEnded(phase.getEndDate(), today))
                                      .map(phase -> new LicenceScheduleTermAndPhases.PhaseDetails(
                                          phase.getId().toString(),
                                          phase.getPhaseType().getDisplayName()
                                      ))
                                      .toList();
  }

  public LicenceScheduleExtensionForm getlicenceScheduleExtensionForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    var requests = licenceScheduleExtensionRepository
        .findAllByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);

    var validTermsAndPhases = getExtendableTermAndPhases(
        scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication()
                                              .getLicenceScheduleDetail());

    if (requests.isEmpty()) {
      return getNewLicenceScheduleExtensionForm(validTermsAndPhases);
    }

    return extensionRequestsToForm(requests, validTermsAndPhases);
  }


  public boolean canExtendMoreThanOneOption(List<LicenceScheduleTermAndPhases> validTermsAndPhases) {
    if (validTermsAndPhases == null) {
      return false;
    }

    var totalCount = validTermsAndPhases.stream()
                                         .mapToLong(term -> {
                                           var termCount = term.termId() != null ? 1 : 0;
                                           var phaseCount = term.phases() != null ? term.phases().size() : 0;
                                           return termCount + phaseCount;
                                         })
                                         .sum();

    return totalCount > 1;
  }

  private boolean hasNotEnded(LocalDate endDate, LocalDate today) {
    return !endDate.isBefore(today);
  }

  private boolean isFinalTerm(
      LicenceScheduleTerm currentTerm,
      List<LicenceScheduleTerm> allExistingTerms
  ) {

    var finalGrantedTermType = allExistingTerms
        .stream()
        .map(LicenceScheduleTerm::getTermType)
        .max(Comparator.comparingInt(TermType::getDisplayOrder));

    return finalGrantedTermType
        .map(lastTermType -> currentTerm.getTermType()
                                        .getDisplayOrder() == lastTermType.getDisplayOrder()
        )
        .orElse(false);
  }

  private boolean isActive(LocalDate startDate, LocalDate endDate, LocalDate today) {
    return !today.isBefore(startDate) && !today.isAfter(endDate);
  }


  @Transactional
  public void saveExtensionForm(
      LicenceScheduleExtensionForm licenceScheduleExtensionForm,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    Set<String> selectedIds = new HashSet<>();

    saveSelectedExtensions(
        licenceScheduleExtensionForm.getSelectedPhase(),
        true,
        licenceScheduleExtensionForm,
        scheduleWorkProgrammeApplicationDetail,
        selectedIds
    );

    saveSelectedExtensions(
        licenceScheduleExtensionForm.getSelectedTerm(),
        false,
        licenceScheduleExtensionForm,
        scheduleWorkProgrammeApplicationDetail,
        selectedIds
    );

    if (selectedIds.isEmpty() && licenceScheduleExtensionForm.getExtensionDuration() != null
        && licenceScheduleExtensionForm.getExtensionDuration().size() == 1) {
      saveSingleExtensionRequest(licenceScheduleExtensionForm, scheduleWorkProgrammeApplicationDetail, selectedIds);
    }

    deleteUnselectedExtensionRequest(scheduleWorkProgrammeApplicationDetail, selectedIds);
  }

  private void saveSelectedExtensions(
      Map<String, Boolean> selectedMap,
      boolean isPhase,
      LicenceScheduleExtensionForm form,
      ScheduleWorkProgrammeApplicationDetail detail,
      Set<String> selectedIds
  ) {

    if (selectedMap == null || selectedMap.isEmpty()) {
      return;
    }

    for (Map.Entry<String, Boolean> entry : selectedMap.entrySet()) {
      if (BooleanUtils.isTrue(entry.getValue())) {
        var idStr = entry.getKey();
        selectedIds.add(idStr);
        var id = UUID.fromString(idStr);

        ThreeFieldDuration duration = form.getExtensionDuration()
                                          .get(idStr)
                                          .toThreeFieldDuration();

        saveExtensionRequest(detail, id, duration, isPhase);
      }
    }
  }

  private void saveSingleExtensionRequest(
      LicenceScheduleExtensionForm form,
      ScheduleWorkProgrammeApplicationDetail detail,
      Set<String> selectedIds
  ) {
    var idStr = form.getExtensionDuration()
                       .keySet()
                       .iterator()
                       .next();
    selectedIds.add(idStr);
    var id = UUID.fromString(idStr);
    ThreeFieldDuration duration = form.getExtensionDuration()
                                      .get(idStr)
                                      .toThreeFieldDuration();

    var hasPhase = licenceSchedulePhaseRepository.existsById(id);
    var hasTerm = licenceScheduleTermRepository.existsById(id);

    if (hasPhase || hasTerm) {
      saveExtensionRequest(detail, id, duration, hasPhase);
    }
  }

  private void saveExtensionRequest(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      UUID id,
      ThreeFieldDuration threeFieldDuration,
      boolean isPhase
  ) {
    Optional<LicenceScheduleExtensionRequest> optionalLicenceScheduleExtensionRequest;
    LicenceScheduleExtensionRequest licenceScheduleExtensionRequest;

    if (isPhase) {
      optionalLicenceScheduleExtensionRequest = licenceScheduleExtensionRepository
          .findByScheduleWorkProgrammeApplicationDetailsAndLicenceSchedulePhaseId(
              scheduleWorkProgrammeApplicationDetail, id);
    } else {
      optionalLicenceScheduleExtensionRequest = licenceScheduleExtensionRepository
          .findByScheduleWorkProgrammeApplicationDetailsAndLicenceScheduleTermId(
              scheduleWorkProgrammeApplicationDetail, id);
    }

    licenceScheduleExtensionRequest = optionalLicenceScheduleExtensionRequest.orElse(
        new LicenceScheduleExtensionRequest());

    licenceScheduleExtensionRequest.setExtensionDuration(threeFieldDuration);
    licenceScheduleExtensionRequest.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);

    if (isPhase) {
      LicenceSchedulePhase phase = licenceSchedulePhaseRepository
          .findById(id)
          .orElseThrow(() -> new IllegalArgumentException("LicenceSchedulePhase not found for ID: " + id));

      licenceScheduleExtensionRequest.setLicenceSchedulePhase(phase);
      licenceScheduleExtensionRequest.setLicenceScheduleTerm(null);
    } else {
      LicenceScheduleTerm term = licenceScheduleTermRepository
          .findById(id)
          .orElseThrow(() -> new IllegalArgumentException("LicenceScheduleTerm not found for ID: " + id));

      licenceScheduleExtensionRequest.setLicenceScheduleTerm(term);
      licenceScheduleExtensionRequest.setLicenceSchedulePhase(null);

    }

    licenceScheduleExtensionRepository.save(licenceScheduleExtensionRequest);
  }

  private void deleteUnselectedExtensionRequest(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail, Set<String> selectedIds) {
    List<LicenceScheduleExtensionRequest> existingRequests =
        licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail);

    existingRequests.stream()
                    .filter(extensionRequest -> {
                      String id = extensionRequest.getLicenceSchedulePhase() != null
                                  ? extensionRequest.getLicenceSchedulePhase()
                                                    .getId()
                                                    .toString()
                                  : extensionRequest.getLicenceScheduleTerm()
                                                    .getId()
                                                    .toString();

                      return !selectedIds.contains(id);
                    })
                    .forEach(licenceScheduleExtensionRepository::delete);
  }

  private void populateExtensionDurationMap(
      LicenceScheduleExtensionForm form,
      String id,
      ThreeFieldDuration durationValue
  ) {
    var inputName = "extensionDuration[" + id + "]";

    ThreeFieldDurationInput durationInput = new ThreeFieldDurationInput(inputName, "extension");

    if (durationValue != null) {
      durationInput.setFromThreeFieldDuration(durationValue);
    }

    form.getExtensionDuration().put(id, durationInput);
  }

  public LicenceScheduleExtensionForm getNewLicenceScheduleExtensionForm(
      List<LicenceScheduleTermAndPhases> validTermsAndPhases
  ) {
    var form = new LicenceScheduleExtensionForm();

    for (LicenceScheduleTermAndPhases term : validTermsAndPhases) {
      if (term.termId() != null) {
        String termKey = term.termId();

        form.getSelectedTerm().put(termKey, false);

        populateExtensionDurationMap(form, termKey, null);
      }

      if (term.phases() != null) {
        for (LicenceScheduleTermAndPhases.PhaseDetails phase : term.phases()) {
          String phaseKey = phase.phaseId();

          form.getSelectedPhase().put(phaseKey, false);

          populateExtensionDurationMap(form, phaseKey, null);
        }
      }
    }
    return form;
  }

  private LicenceScheduleExtensionForm extensionRequestsToForm(
      List<LicenceScheduleExtensionRequest> requests,
      List<LicenceScheduleTermAndPhases> validTermsAndPhases
  ) {
    var form = getNewLicenceScheduleExtensionForm(validTermsAndPhases);

    for (LicenceScheduleExtensionRequest request : requests) {
      String termOrPhaseId = "";

      if (request.getLicenceSchedulePhase() != null) {
        termOrPhaseId = request.getLicenceSchedulePhase().getId().toString();
        form.getSelectedPhase().put(termOrPhaseId, true);
      } else if (request.getLicenceScheduleTerm() != null) {
        termOrPhaseId = request.getLicenceScheduleTerm().getId().toString();
        form.getSelectedTerm().put(termOrPhaseId, true);
      }
      populateExtensionDurationMap(form, termOrPhaseId, request.getExtensionDuration());
    }
    return form;
  }




}