package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleTermFormService {

  private final LicenceScheduleTermRepository licenceScheduleTermRepository;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;

  public LicenceScheduleTermFormService(
      LicenceScheduleTermRepository licenceScheduleTermRepository,
      LicenceScheduleCalculationService licenceScheduleCalculationService
  ) {
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
  }

  @Transactional
  public void saveTermFromForm(
      LicenceScheduleTermForm licenceScheduleTermForm,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm.setTermType(licenceScheduleTermForm.getTermType());
    licenceScheduleTerm.setTermDuration(licenceScheduleTermForm.getTermDuration().toThreeFieldDuration());
    licenceScheduleTermRepository.save(licenceScheduleTerm);

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }
}
