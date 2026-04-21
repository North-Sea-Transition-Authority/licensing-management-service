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

    return licenceScheduleTerms
        .stream()
        .filter(term -> isCurrentlyActive(term.getStartDate(), term.getEndDate()))
        .findFirst()
        .orElse(null);
  }

  public LicenceSchedulePhase getCurrentPhase(LicenceScheduleTerm licenceScheduleTerm) {
    var licenceSchedulePhases = licenceSchedulePhaseService.getActivePhasesByTerm(licenceScheduleTerm);

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

  private Optional<LicenceScheduleTerm> getNextTerm(List<LicenceScheduleTerm> terms, LicenceScheduleTerm currentTerm) {
    return terms.stream()
        .filter(term -> term.getStartDate().isAfter(currentTerm.getStartDate()))
        .min(Comparator.comparing(LicenceScheduleTerm::getStartDate));
  }

  private Optional<LicenceSchedulePhase> getNextPhase(
      List<LicenceSchedulePhase> phases,
      LicenceSchedulePhase currentPhase
  ) {
    return phases.stream()
        .filter(phase -> phase.getStartDate().isAfter(currentPhase.getStartDate()))
        .min(Comparator.comparing(LicenceSchedulePhase::getStartDate));
  }

  public Optional<LicenceScheduleTerm> getNextTerm(LicenceScheduleDetail licenceScheduleDetail) {
    var currentTerm = getCurrentTerm(licenceScheduleDetail);
    if (currentTerm == null) {
      return Optional.empty();
    }

    var terms = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail);
    return getNextTerm(terms, currentTerm);
  }

  public Optional<LicenceSchedulePhase> getNextPhase(LicenceScheduleDetail licenceScheduleDetail) {
    var currentTerm = getCurrentTerm(licenceScheduleDetail);
    if (currentTerm == null) {
      return Optional.empty();
    }

    var currentPhase = getCurrentPhase(currentTerm);
    if (currentPhase == null) {
      return Optional.empty();
    }

    var phases = licenceSchedulePhaseService.getActivePhasesByTerm(currentTerm);
    return getNextPhase(phases, currentPhase);
  }
}
