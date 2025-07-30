package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

@Service
public class LicenceInternalApiService {

  private final LicenceRepository licenceRepository;

  public LicenceInternalApiService(LicenceRepository licenceRepository) {
    this.licenceRepository = licenceRepository;
  }

  List<LicenceJson> searchLicencesByReference(String searchTerm) {
    return licenceRepository.findAllByLicenceReferenceContainingIgnoreCase(searchTerm).stream()
        .map(this::toLicenceJson)
        .toList();
  }

  List<LicenceJson> searchLicencesByReferenceAndType(String searchTerm, LicenceType type) {
    return licenceRepository.findAllByLicenceReferenceContainingIgnoreCaseAndType(searchTerm, type).stream()
        .map(this::toLicenceJson)
        .toList();
  }

  private LicenceJson toLicenceJson(Licence licence) {
    return new LicenceJson(
        licence.getId(),
        licence.getLicenceReference()
    );
  }
}
