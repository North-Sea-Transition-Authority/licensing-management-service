package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney.SelectLicenceForm;

@Service
public class LicenceScheduleService {

  private final LicenceService licenceService;
  private final LicenceScheduleRepository licenceScheduleRepository;

  public LicenceScheduleService(
      LicenceService licenceService,
      LicenceScheduleRepository licenceScheduleRepository
  ) {
    this.licenceService = licenceService;
    this.licenceScheduleRepository = licenceScheduleRepository;
  }

  public boolean doesLicenceScheduleExistForLicence(Licence licence) {
    return licenceScheduleRepository.existsByLicence(licence);
  }

  @Transactional
  public void saveLicenceScheduleFromForm(SelectLicenceForm licenceForm) {
    var licence = licenceService.findLicenceByIdOrThrow(Integer.parseInt(licenceForm.getLicenceId()));

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    licenceScheduleRepository.save(licenceSchedule);
  }

}
