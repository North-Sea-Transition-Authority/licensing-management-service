package uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@Service
public class LicenceScheduleCalculationService {

  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final OtherScheduleEventService otherScheduleEventService;

  public LicenceScheduleCalculationService(
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceScheduleRateService licenceScheduleRateService,
      OtherScheduleEventService otherScheduleEventService
  ) {
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.otherScheduleEventService = otherScheduleEventService;
  }

  @Transactional
  public void calculateAndSaveLicenceScheduleDates(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail).getStartDate();

    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);
    var phases = licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail);

    terms.sort(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    var nextStartDate = licenceStartDate;

    for (var term : terms) {
      var endDate = calculateDurationEndDate(nextStartDate, term.getTermDuration());

      term.setStartDate(nextStartDate);
      term.setEndDate(endDate);

      calculateAndSavePhaseDatesForTerm(phases, term);
      calculateAndSaveWorkProgrammeActivityDatesForTerm(term);
      calculateAndSaveRateStartDatesForTerm(term);
      calculateAndSaveOtherScheduleEventDatesForTerm(term);

      nextStartDate = endDate.plusDays(1);
    }

    licenceScheduleTermService.saveTerms(terms);
  }

  void calculateAndSavePhaseDatesForTerm(
      List<LicenceSchedulePhase> licenceSchedulePhases,
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    var phaseTypes = PhaseType.getPhasesFor(licenceScheduleTerm.getTermType());

    var phasesToCalculate = licenceSchedulePhases.stream()
        .filter(phase -> phaseTypes.contains(phase.getPhaseType()))
        .sorted(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()))
        .toList();

    if (phasesToCalculate.isEmpty()) {
      return;
    }

    var nextStartDate = licenceScheduleTerm.getStartDate();

    for (var phase : phasesToCalculate) {
      var endDate = calculateDurationEndDate(nextStartDate, phase.getPhaseDuration());

      phase.setStartDate(nextStartDate);
      phase.setEndDate(endDate);

      calculateAndSaveWorkProgrammeActivityDatesForPhase(phase);
      calculateAndSaveRateStartDatesForPhase(phase);
      calculateAndSaveOtherScheduleEventDatesForPhase(phase);

      nextStartDate = endDate.plusDays(1);
    }

    licenceSchedulePhaseService.saveLicenceSchedulePhases(phasesToCalculate);
  }

  void calculateAndSaveWorkProgrammeActivityDatesForTerm(LicenceScheduleTerm licenceScheduleTerm) {
    var activities = workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(
        licenceScheduleTerm,
        WorkProgrammeActivityDateOption.RELATIVE_DATE
    );

    if (activities.isEmpty()) {
      return;
    }

    var termStartDate = licenceScheduleTerm.getStartDate();

    for (var activity : activities) {
      activity.setDueDate(calculateRelativeStartDueDate(termStartDate, activity.getRelativeDuration()));
    }

    workProgrammeActivityService.saveWorkProgrammeActivities(activities);
  }

  void calculateAndSaveWorkProgrammeActivityDatesForPhase(LicenceSchedulePhase licenceSchedulePhase) {
    var activities = workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(
        licenceSchedulePhase,
        WorkProgrammeActivityDateOption.RELATIVE_DATE
    );

    if (activities.isEmpty()) {
      return;
    }

    var phaseStartDate = licenceSchedulePhase.getStartDate();

    for (var activity : activities) {
      activity.setDueDate(calculateRelativeStartDueDate(phaseStartDate, activity.getRelativeDuration()));
    }

    workProgrammeActivityService.saveWorkProgrammeActivities(activities);
  }

  void calculateAndSaveRateStartDatesForTerm(LicenceScheduleTerm licenceScheduleTerm) {
    var termStartDate = licenceScheduleTerm.getStartDate();

    var linkedRates = licenceScheduleRateService.getLicenceScheduleRatesForTermAndDefinitionOption(
        licenceScheduleTerm,
        RateDefinitionOption.TERM
    );

    for (var linkedRate : linkedRates) {
      linkedRate.setStartDate(termStartDate);
    }

    var relativeRates = licenceScheduleRateService.getLicenceScheduleRatesForTermAndDefinitionOption(
        licenceScheduleTerm,
        RateDefinitionOption.CUSTOM_PERIOD
    );

    for (var relativeRate : relativeRates) {
      if (relativeRate.getRateRelativeDateOption().equals(RateRelativeDateOption.ON_START_DATE)) {
        relativeRate.setStartDate(termStartDate);
      } else {
        relativeRate.setStartDate(calculateRelativeStartDueDate(termStartDate, relativeRate.getRelativeDuration()));
      }
    }

    licenceScheduleRateService.saveLicenceScheduleRates(linkedRates);
    licenceScheduleRateService.saveLicenceScheduleRates(relativeRates);
  }

  void calculateAndSaveRateStartDatesForPhase(LicenceSchedulePhase licenceSchedulePhase) {
    var phaseStartDate = licenceSchedulePhase.getStartDate();

    var linkedRates = licenceScheduleRateService.getLicenceScheduleRatesForPhaseAndDefinitionOption(
        licenceSchedulePhase,
        RateDefinitionOption.PHASE
    );

    for (var linkedRate : linkedRates) {
      linkedRate.setStartDate(phaseStartDate);
    }

    var relativeRates = licenceScheduleRateService.getLicenceScheduleRatesForPhaseAndDefinitionOption(
        licenceSchedulePhase,
        RateDefinitionOption.CUSTOM_PERIOD
    );

    for (var relativeRate : relativeRates) {
      if (relativeRate.getRateRelativeDateOption().equals(RateRelativeDateOption.ON_START_DATE)) {
        relativeRate.setStartDate(phaseStartDate);
      } else {
        relativeRate.setStartDate(calculateRelativeStartDueDate(phaseStartDate, relativeRate.getRelativeDuration()));
      }
    }

    licenceScheduleRateService.saveLicenceScheduleRates(linkedRates);
    licenceScheduleRateService.saveLicenceScheduleRates(relativeRates);
  }

  void calculateAndSaveOtherScheduleEventDatesForTerm(LicenceScheduleTerm licenceScheduleTerm) {
    var events = otherScheduleEventService.getScheduleEventsByTermAndDateOption(
        licenceScheduleTerm,
        OtherScheduleEventDateOption.RELATIVE_DATE
    );

    if (events.isEmpty()) {
      return;
    }

    var termStartDate = licenceScheduleTerm.getStartDate();

    for (var event : events) {
      event.setEventDate(calculateRelativeStartDueDate(termStartDate, event.getRelativeDuration()));
    }

    otherScheduleEventService.saveScheduleEvents(events);
  }

  void calculateAndSaveOtherScheduleEventDatesForPhase(LicenceSchedulePhase licenceSchedulePhase) {
    var events = otherScheduleEventService.getScheduleEventsByPhaseAndDateOption(
        licenceSchedulePhase,
        OtherScheduleEventDateOption.RELATIVE_DATE
    );

    if (events.isEmpty()) {
      return;
    }

    var phaseStartDate = licenceSchedulePhase.getStartDate();

    for (var event : events) {
      event.setEventDate(calculateRelativeStartDueDate(phaseStartDate, event.getRelativeDuration()));
    }

    otherScheduleEventService.saveScheduleEvents(events);
  }

  LocalDate calculateDurationEndDate(LocalDate startDate, ThreeFieldDuration duration) {
    var endDate = startDate
        .plusYears(duration.years())
        .plusMonths(duration.months())
        .plusDays(duration.days());

    var yearOrMonthDuration = duration.years() > 0 || duration.months() > 0;

    return yearOrMonthDuration
        ? endDate.minusDays(1)
        : endDate;
  }

  public LocalDate calculateRelativeStartDueDate(LocalDate startDate, ThreeFieldDuration duration) {
    return startDate
        .plusYears(duration.years())
        .plusMonths(duration.months())
        .plusDays(duration.days());
  }

  public Map<UUID, StartEndDates> calculateRateEndDatesForDisplay(
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var rates = licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail);

    var sortedRates = rates.stream()
        .sorted(Comparator.comparing(this::getStartDateForRate))
        .toList();

    var rateDateMap = new LinkedHashMap<UUID, StartEndDates>();
    for (var i = 0; i < sortedRates.size(); i++) {
      var rate = sortedRates.get(i);
      LocalDate endDate;

      if (i < sortedRates.size() - 1) {
        var dayBeforeNextRate = getStartDateForRate(sortedRates.get(i + 1)).minusDays(1);
        endDate = getEndDateForRate(rate, dayBeforeNextRate);
      } else {
        endDate = getFinalTermEndDate(licenceScheduleDetail);
      }
      rateDateMap.put(rate.getId(), new StartEndDates(getStartDateForRate(rate), endDate));
    }

    return rateDateMap;
  }

  private LocalDate getStartDateForRate(LicenceScheduleRate rate) {
    return switch (rate.getRateDefinitionOption()) {
      case TERM -> rate.getLicenceScheduleTerm().getStartDate();
      case PHASE -> rate.getLicenceSchedulePhase().getStartDate();
      case CUSTOM_PERIOD -> rate.getStartDate();
    };
  }

  private LocalDate getEndDateForRate(LicenceScheduleRate rate, LocalDate dayBeforeNextRate) {
    return switch (rate.getRateDefinitionOption()) {
      case TERM -> rate.getLicenceScheduleTerm().getEndDate();
      case PHASE -> rate.getLicenceSchedulePhase().getEndDate();
      case CUSTOM_PERIOD -> dayBeforeNextRate;
    };
  }

  private LocalDate getFinalTermEndDate(LicenceScheduleDetail licenceScheduleDetail) {
    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);

    terms.sort(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    return terms.getLast().getEndDate();
  }

}
