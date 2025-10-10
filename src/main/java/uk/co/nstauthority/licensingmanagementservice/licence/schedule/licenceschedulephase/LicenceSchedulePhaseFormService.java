package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceSchedulePhaseFormService {

  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;

  public LicenceSchedulePhaseFormService(
      LicenceSchedulePhaseRepository licenceSchedulePhaseRepository,
      LicenceScheduleCalculationService licenceScheduleCalculationService
  ) {
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
  }

  @Transactional
  public void savePhaseFromForm(
      LicenceSchedulePhaseForm licenceSchedulePhaseForm,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceSchedulePhase.setPhaseType(licenceSchedulePhaseForm.getPhaseType());
    licenceSchedulePhase.setPhaseDuration(licenceSchedulePhaseForm.getPhaseDuration().toThreeFieldDuration());
    licenceSchedulePhase.setComments(licenceSchedulePhaseForm.getComments());
    licenceSchedulePhase.setStatus(LicenceScheduleEventStatus.ACTIVE);
    licenceSchedulePhaseRepository.save(licenceSchedulePhase);

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

}
