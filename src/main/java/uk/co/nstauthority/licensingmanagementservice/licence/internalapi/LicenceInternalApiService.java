package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;

@Service
public class LicenceInternalApiService {

  private final LicenceRepository licenceRepository;

  private final LicenceScheduleService licenceScheduleService;

  public LicenceInternalApiService(
      LicenceRepository licenceRepository,
      LicenceScheduleService licenceScheduleService
  ) {
    this.licenceRepository = licenceRepository;
    this.licenceScheduleService = licenceScheduleService;
  }

  List<LicenceJson> searchLicencesByReference(String searchTerm) {
    return licenceRepository.findAllByLicenceReferenceContainingIgnoreCase(searchTerm).stream()
        .map(this::toLicenceJson)
        .toList();
  }

  List<LicenceJson> searchLicencesWithSchedulesByReferenceAndType(String searchTerm, LicenceType type) {
    return licenceScheduleService.searchAllSchedulesByLicenceRefAndType(searchTerm, type).stream()
        .map(LicenceSchedule::getLicence)
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
