package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

  public Licence findByLicenceReferenceOrThrow(String licenceReference) {
    return findLicenceByReference(licenceReference)
        .orElseThrow(() -> new LmsEntityNotFoundException("Could not find licence with ref: %s".formatted(licenceReference)));
  }

  @Transactional
  public Iterable<Licence> saveLicences(Collection<Licence> licences) {
    return licenceRepository.saveAll(licences);
  }

  public Set<Integer> getExistingLicenceIds(Collection<Integer> licenceIds) {
    return licenceRepository.findAllById(licenceIds).stream()
        .map(Licence::getId)
        .collect(Collectors.toSet());
  }

  public List<Licence> getLicencesByIds(Collection<Integer> licenceIds) {
    return licenceRepository.findAllById(licenceIds);
  }

  /**
   * Returns the subset of the given licence references that already belong to a licence, of any type. Used by
   * migrations to avoid inserting a licence that has already been migrated.
   */
  public Set<String> getExistingLicenceReferences(Collection<String> licenceReferences) {
    return licenceRepository.findAllByLicenceReferenceIn(licenceReferences).stream()
        .map(Licence::getLicenceReference)
        .collect(Collectors.toSet());
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

  public boolean isCarbonStorageLicence(Licence licence) {
    return LicenceType.CARBON_STORAGE.equals(licence.getType());
  }

  public List<Licence> searchLicencesByReferenceAndTypes(
      String searchTerm,
      List<LicenceType> types
  ) {
    return licenceRepository.findAllByLicenceReferenceContainingIgnoreCaseAndTypeIn(
        searchTerm,
        types
    );
  }
}
