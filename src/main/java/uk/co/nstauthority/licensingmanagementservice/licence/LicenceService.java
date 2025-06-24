package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.Collection;
import org.springframework.stereotype.Service;

@Service
public class LicenceService {

  private final LicenceRepository licenceRepository;

  public LicenceService(LicenceRepository licenceRepository) {
    this.licenceRepository = licenceRepository;
  }

  @Transactional
  public Iterable<Licence> saveLicences(Collection<Licence> licences) {
    return licenceRepository.saveAll(licences);
  }

  // Generate the next licence id. If there are none, start at 10000 to leave a buffer for pears managed licence ids.
  // We are manually generating ids because @GeneratedValue prevents saving fixed ids which we need to do to when
  // pulling licence data from pears to preserve pears licence ids.
  public Integer getNextLicenceId() {
    var maxLicence = licenceRepository.findTopByOrderByIdDesc();

    if (maxLicence.isEmpty()) {
      return 10000;
    }

    var maxIdValue = maxLicence.get().getId();

    return maxIdValue >= 10000
        ? maxIdValue + 1
        : 10000;
  }
}
