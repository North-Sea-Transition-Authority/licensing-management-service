package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Service
public class LicenceScheduleService {

  private final LicenceScheduleRepository licenceScheduleRepository;

  public LicenceScheduleService(LicenceScheduleRepository licenceScheduleRepository) {
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

  /**
   * Returns the ids of the given licences that already have a schedule. A licence has at most one schedule, so
   * migrations use this to avoid giving a licence a second one.
   */
  public Set<Integer> getIdsOfLicencesWithASchedule(Collection<Licence> licences) {
    if (licences.isEmpty()) {
      return Set.of();
    }

    return licenceScheduleRepository.findAllByLicenceIn(licences).stream()
        .map(licenceSchedule -> licenceSchedule.getLicence().getId())
        .collect(Collectors.toSet());
  }
}
