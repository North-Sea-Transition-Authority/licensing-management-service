package uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation;

import com.google.common.annotations.VisibleForTesting;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;

@Service
public class LicenceScheduleCalculationService {

  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleTermService licenceScheduleTermService;

  public LicenceScheduleCalculationService(
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleTermService licenceScheduleTermService
  ) {
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleTermService = licenceScheduleTermService;
  }

  @Transactional
  public void calculateAndSaveLicenceScheduleDates(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail).getStartDate();

    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);

    terms.sort(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    var nextStartDate = licenceStartDate;

    for (var term : terms) {
      var endDate = calculateEndDate(nextStartDate, term.getTermDuration());

      term.setStartDate(nextStartDate);
      term.setEndDate(endDate);

      nextStartDate = endDate.plusDays(1);
    }

    licenceScheduleTermService.saveTerms(terms);
  }

  @VisibleForTesting
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
