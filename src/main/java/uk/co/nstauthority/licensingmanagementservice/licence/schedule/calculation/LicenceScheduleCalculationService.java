package uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@Service
public class LicenceScheduleCalculationService {

  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final WorkProgrammeActivityService workProgrammeActivityService;

  public LicenceScheduleCalculationService(
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      WorkProgrammeActivityService workProgrammeActivityService
  ) {
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.workProgrammeActivityService = workProgrammeActivityService;
  }

  @Transactional
  public void calculateAndSaveLicenceScheduleDates(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail).getStartDate();

    var terms = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail);
    var phases = licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail);

    terms.sort(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    var nextStartDate = licenceStartDate;

    for (var term : terms) {
      var endDate = calculateDurationEndDate(nextStartDate, term.getTermDuration());

      term.setStartDate(nextStartDate);
      term.setEndDate(endDate);

      calculateAndSavePhaseDatesForTerm(phases, term);
      calculateAndSaveWorkProgrammeActivityDatesForTerm(term);

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

      nextStartDate = endDate.plusDays(1);
    }

    licenceSchedulePhaseService.saveLicenceSchedulePhases(phasesToCalculate);
  }

  void calculateAndSaveWorkProgrammeActivityDatesForTerm(LicenceScheduleTerm licenceScheduleTerm) {
    var activities = workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(
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
    var activities = workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(
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

  LocalDate calculateRelativeStartDueDate(LocalDate startDate, ThreeFieldDuration duration) {
    return startDate
        .plusYears(duration.years())
        .plusMonths(duration.months())
        .plusDays(duration.days());
  }

}
