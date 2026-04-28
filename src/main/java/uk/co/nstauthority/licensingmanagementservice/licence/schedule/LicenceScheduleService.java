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

@Service
public class LicenceScheduleService {

  private final LicenceScheduleRepository licenceScheduleRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;
  private final Clock clock;

  public LicenceScheduleService(
      LicenceScheduleRepository licenceScheduleRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceTypeRulesResolver licenceTypeRulesResolver,
      Clock clock
  ) {
    this.licenceScheduleRepository = licenceScheduleRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
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
    var licenceScheduleTerms = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(
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
    var licenceSchedulePhases = licenceSchedulePhaseService.getActivePhasesByTerm(licenceScheduleTerm);
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

    var terms = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail);
    return getNextTerm(terms, currentTerm)
        .map(nextTerm -> resolveStartDateForTerm(nextTerm, licenceTypeHasPhases));
  }

  private Optional<LocalDate> getNextPhaseStartDateInTerm(LicenceScheduleTerm term) {
    var possibleCurrentPhase = Optional.ofNullable(getCurrentPhase(term));

    if (possibleCurrentPhase.isEmpty()) {
      return Optional.empty();
    }

    var currentPhase = possibleCurrentPhase.get();
    var phases = licenceSchedulePhaseService.getActivePhasesByTerm(term);
    return getNextPhase(phases, currentPhase).map(LicenceSchedulePhase::getStartDate);
  }

  private LocalDate resolveStartDateForTerm(LicenceScheduleTerm term, boolean licenceTypeHasPhases) {
    if (licenceTypeHasPhases) {
      var firstPhaseStartDate = licenceSchedulePhaseService.getActivePhasesByTerm(term)
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
    var terms = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail);
    var currentTerm = getCurrentTerm(terms);
    var nextTerm = currentTerm != null ? getNextTerm(terms, currentTerm).orElse(null) : null;

    LicenceSchedulePhase currentPhase = null;
    LicenceSchedulePhase nextPhase = null;

    if (currentTerm != null) {
      var phases = licenceSchedulePhaseService.getActivePhasesByTerm(currentTerm);
      currentPhase = getCurrentPhase(phases);

      if (currentPhase != null) {
        nextPhase = getNextPhase(phases, currentPhase).orElse(null);
      }
    }

    return new ScheduleState(currentTerm, currentPhase, nextTerm, nextPhase);
  }
}