package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@Service
public class LicenceScheduleService {

  private final LicenceScheduleRepository licenceScheduleRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final Clock clock;

  public LicenceScheduleService(
      LicenceScheduleRepository licenceScheduleRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceTypeRulesResolver licenceTypeRulesResolver,
      WorkProgrammeActivityService workProgrammeActivityService,
      Clock clock
  ) {
    this.licenceScheduleRepository = licenceScheduleRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.clock = clock;
  }

  @Transactional
  public LicenceSchedule getOrCreateNewLicenceScheduleForLicence(Licence licence) {
    return licenceScheduleRepository.findByLicence(licence)
        .orElseGet(() -> saveNewLicenceSchedule(licence));
  }

  private LicenceSchedule saveNewLicenceSchedule(Licence licence) {
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    return licenceScheduleRepository.save(licenceSchedule);
  }

  @Transactional
  public Iterable<LicenceSchedule> saveLicenceSchedules(List<LicenceSchedule> licenceSchedules) {
    return licenceScheduleRepository.saveAll(licenceSchedules);
  }

  public LicenceScheduleTerm getCurrentTerm(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceScheduleTerms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(
        licenceScheduleDetail);
    return getCurrentTerm(licenceScheduleTerms);
  }

  public LicenceScheduleTerm getCurrentTerm(List<LicenceScheduleTerm> licenceScheduleTerms) {
    return licenceScheduleTerms
        .stream()
        .filter(term -> isCurrentlyActive(term.getStartDate(), term.getEndDate()))
        .findFirst()
        .orElse(null);
  }

  public LicenceSchedulePhase getCurrentPhase(LicenceScheduleTerm licenceScheduleTerm) {
    var licenceSchedulePhases = licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm);
    return getCurrentPhase(licenceSchedulePhases);
  }

  public LicenceSchedulePhase getCurrentPhase(List<LicenceSchedulePhase> licenceSchedulePhases) {
    return licenceSchedulePhases
        .stream()
        .filter(phase -> isCurrentlyActive(phase.getStartDate(), phase.getEndDate()))
        .findFirst()
        .orElse(null);
  }

  public LicenceSchedulePhase getCurrentPhase(LicenceScheduleDetail licenceScheduleDetail) {
    var currentTerm = getCurrentTerm(licenceScheduleDetail);
    return currentTerm == null ? null : getCurrentPhase(currentTerm);
  }

  private boolean isCurrentlyActive(LocalDate startDate, LocalDate endDate) {
    LocalDate today = LocalDate.now(clock);

    if (startDate == null || endDate == null) {
      throw new IllegalArgumentException("Start date and end date cannot be null");
    }

    boolean hasStarted = startDate.isBefore(today) || startDate.isEqual(today);
    boolean hasNotEnded = endDate.isAfter(today);

    return hasStarted && hasNotEnded;
  }

  public Optional<LocalDate> getNextTermPhaseStartDate(LicenceScheduleDetail licenceScheduleDetail) {
    var possibleCurrentTerm = Optional.ofNullable(getCurrentTerm(licenceScheduleDetail));

    if (possibleCurrentTerm.isEmpty()) {
      return Optional.empty();
    }

    var currentTerm = possibleCurrentTerm.get();
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();
    var licenceTypeHasPhases = licenceTypeRulesResolver.hasPhases(licence.getType());

    if (licenceTypeHasPhases) {
      var nextPhaseStartDate = getNextPhaseStartDateInTerm(currentTerm);
      if (nextPhaseStartDate.isPresent()) {
        return nextPhaseStartDate;
      }
    }

    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);
    return getNextTerm(terms, currentTerm)
        .map(nextTerm -> resolveStartDateForTerm(nextTerm, licenceTypeHasPhases));
  }

  private Optional<LocalDate> getNextPhaseStartDateInTerm(LicenceScheduleTerm term) {
    var possibleCurrentPhase = Optional.ofNullable(getCurrentPhase(term));

    if (possibleCurrentPhase.isEmpty()) {
      return Optional.empty();
    }

    var currentPhase = possibleCurrentPhase.get();
    var phases = licenceSchedulePhaseService.getPhasesByTerm(term);
    return getNextPhase(phases, currentPhase).map(LicenceSchedulePhase::getStartDate);
  }

  private LocalDate resolveStartDateForTerm(LicenceScheduleTerm term, boolean licenceTypeHasPhases) {
    if (licenceTypeHasPhases) {
      var firstPhaseStartDate = licenceSchedulePhaseService.getPhasesByTerm(term)
          .stream()
          .min(Comparator.comparing(LicenceSchedulePhase::getStartDate))
          .map(LicenceSchedulePhase::getStartDate)
          .orElse(null);
      if (firstPhaseStartDate != null) {
        return firstPhaseStartDate;
      }
    }
    return term.getStartDate();
  }

  public Optional<LicenceScheduleTerm> getNextTerm(List<LicenceScheduleTerm> terms, LicenceScheduleTerm currentTerm) {
    return terms.stream()
        .filter(term -> term.getStartDate().isAfter(currentTerm.getStartDate()))
        .min(Comparator.comparing(LicenceScheduleTerm::getStartDate));
  }

  public Optional<LicenceSchedulePhase> getNextPhase(
      List<LicenceSchedulePhase> phases,
      LicenceSchedulePhase currentPhase
  ) {
    return phases.stream()
        .filter(phase -> phase.getStartDate().isAfter(currentPhase.getStartDate()))
        .min(Comparator.comparing(LicenceSchedulePhase::getStartDate));
  }

  public ScheduleState getScheduleState(LicenceScheduleDetail licenceScheduleDetail) {
    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);
    var currentTerm = getCurrentTerm(terms);

    LicenceSchedulePhase currentPhase = null;
    LicenceScheduleTerm nextTerm = null;
    LicenceSchedulePhase nextPhase = null;

    if (currentTerm != null) {
      var currentTermPhases = licenceSchedulePhaseService.getPhasesByTerm(currentTerm);
      currentPhase = getCurrentPhase(currentTermPhases);

      nextPhase = getNextPhaseInSameTerm(currentTermPhases, currentPhase);

      if (nextPhase != null) {
        nextTerm = currentTerm;
      } else {
        nextTerm = getNextTerm(terms, currentTerm).orElse(null);
        nextPhase = getFirstPhaseOfTerm(nextTerm).orElse(null);
      }
    }

    return new ScheduleState(currentTerm, currentPhase, nextTerm, nextPhase);
  }

  private LicenceSchedulePhase getNextPhaseInSameTerm(
      List<LicenceSchedulePhase> currentTermPhases,
      LicenceSchedulePhase currentPhase
  ) {
    if (currentPhase == null) {
      return null;
    }
    return getNextPhase(currentTermPhases, currentPhase).orElse(null);
  }

  private Optional<LicenceSchedulePhase> getFirstPhaseOfTerm(LicenceScheduleTerm term) {
    if (term == null) {
      return Optional.empty();
    }
    return licenceSchedulePhaseService.getPhasesByTerm(term).stream()
        .sorted(Comparator.comparing(LicenceSchedulePhase::getStartDate))
        .toList()
        .stream()
        .findFirst();
  }

  public boolean hasCurrentWorkProgrammeActivities(LicenceScheduleDetail licenceScheduleDetail) {
    var scheduleState = getScheduleState(licenceScheduleDetail);

    if (scheduleState.currentPhase() != null) {
      return workProgrammeActivityService.hasActivitiesForPhase(scheduleState.currentPhase());
    }

    if (scheduleState.currentTerm() != null) {
      return workProgrammeActivityService.hasActivitiesForTerm(scheduleState.currentTerm());
    }

    return false;
  }

  public List<WorkProgrammeActivityView> getCurrentWorkProgrammeActivitiesViews(
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var scheduleState = getScheduleState(licenceScheduleDetail);

    if (scheduleState.currentPhase() != null) {
      return workProgrammeActivityService.getLicenceWorkProgramActivitiesViewsForGivenPhase(scheduleState.currentPhase());
    }

    if (scheduleState.currentTerm() != null) {
      return workProgrammeActivityService.getLicenceWorkProgramActivitiesViewsForGivenTerm(scheduleState.currentTerm());
    }

    return List.of();
  }

  public String formatTermPhaseDisplay(LicenceScheduleTerm term, LicenceSchedulePhase phase) {
    if (term == null) {
      return null;
    }
    if (phase != null) {
      return String.format("%s (%s)", phase.getPhaseType().getDisplayName(), term.getTermType().getDisplayName());
    }
    return term.getTermType().getDisplayName();
  }
}