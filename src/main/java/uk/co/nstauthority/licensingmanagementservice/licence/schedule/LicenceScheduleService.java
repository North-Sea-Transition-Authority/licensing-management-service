package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import jakarta.transaction.Transactional;
import java.util.List;
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

}
