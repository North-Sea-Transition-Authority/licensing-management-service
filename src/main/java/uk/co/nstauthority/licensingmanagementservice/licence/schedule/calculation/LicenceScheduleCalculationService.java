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

@Service
public class LicenceScheduleCalculationService {

  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;

  public LicenceScheduleCalculationService(
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService
  ) {
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
  }

  @Transactional
  public void calculateAndSaveLicenceScheduleDates(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail).getStartDate();

    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);
    var phases = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail);

    terms.sort(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    var nextStartDate = licenceStartDate;

    for (var term : terms) {
      var endDate = calculateEndDate(nextStartDate, term.getTermDuration());

      term.setStartDate(nextStartDate);
      term.setEndDate(endDate);

      calculateAndSavePhaseDatesForTerm(phases, term);

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
      var endDate = calculateEndDate(nextStartDate, phase.getPhaseDuration());

      phase.setStartDate(nextStartDate);
      phase.setEndDate(endDate);

      nextStartDate = endDate.plusDays(1);
    }

    licenceSchedulePhaseService.saveLicenceSchedulePhases(phasesToCalculate);
  }

  LocalDate calculateEndDate(LocalDate startDate, ThreeFieldDuration duration) {
    var endDate = startDate
        .plusYears(duration.years())
        .plusMonths(duration.months())
        .plusDays(duration.days());

    var yearOrMonthDuration = duration.years() > 0 || duration.months() > 0;

    return yearOrMonthDuration
        ? endDate.minusDays(1)
        : endDate;
  }

}
