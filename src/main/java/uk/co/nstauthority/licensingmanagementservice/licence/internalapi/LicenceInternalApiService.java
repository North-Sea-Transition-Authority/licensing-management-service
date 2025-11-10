package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;

@Service
public class LicenceInternalApiService {


  private final LicenceScheduleService licenceScheduleService;

  public LicenceInternalApiService(
      LicenceScheduleService licenceScheduleService
  ) {
    this.licenceScheduleService = licenceScheduleService;
  }

  List<LicenceJson> searchLicencesWithSchedulesByReferenceAndType(String searchTerm, LicenceType type) {
    return licenceScheduleService.searchAllSchedulesByLicenceRefAndType(searchTerm, type).stream()
        .map(LicenceSchedule::getLicence)
        .sorted(Comparator.comparing(Licence::getPrefix).thenComparing(Licence::getLicenceNumber))
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
