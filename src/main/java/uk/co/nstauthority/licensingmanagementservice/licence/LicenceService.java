package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;

@Service
public class LicenceService {

  private final LicenceRepository licenceRepository;

  public LicenceService(LicenceRepository licenceRepository) {
    this.licenceRepository = licenceRepository;
  }

  public List<Licence> getAllLicences() {
    return licenceRepository.findAll();
  }

  public Licence findLicenceByIdOrThrow(Integer id) {
    return licenceRepository.findById(id)
        .orElseThrow(() -> new LmsEntityNotFoundException("licence", id));
  }

  public boolean licenceNumberExistsForType(
      LicenceType licenceType,
      String licenceNumber
  ) {
    return licenceRepository.existsByTypeAndLicenceNumber(licenceType, licenceNumber);
  }

  public Optional<Licence> findLicenceByReference(String licenceReference) {
    return licenceRepository.findByLicenceReference(licenceReference);
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

  public String getLicencePageCaption(Licence licence) {
    var licenceId = licence.getId();
    var licenceType = licence.getType();
    return licenceType.getDisplayName() + " - " + findLicenceByIdOrThrow(licenceId).getLicenceReference();
  }
}
