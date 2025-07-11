package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Service
public class LicenceScheduleService {

  private final LicenceScheduleRepository licenceScheduleRepository;

  public LicenceScheduleService(
      LicenceScheduleRepository licenceScheduleRepository
  ) {
    this.licenceScheduleRepository = licenceScheduleRepository;
  }

  public Optional<LicenceSchedule> getLicenceScheduleByLicence(Licence licence) {
    return licenceScheduleRepository.findByLicence(licence);
  }

  public boolean doesLicenceScheduleExistForLicence(Licence licence) {
    return licenceScheduleRepository.existsByLicence(licence);
  }

  @Transactional
  public LicenceSchedule createNewLicenceScheduleForLicence(Licence licence) {
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    return licenceScheduleRepository.save(licenceSchedule);
  }

}
