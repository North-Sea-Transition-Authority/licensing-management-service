package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@Service
public class LicenceInternalApiService {


  private final LicenceScheduleDetailService licenceScheduleDetailService;

  public LicenceInternalApiService(
      LicenceScheduleDetailService licenceScheduleDetailService
  ) {
    this.licenceScheduleDetailService = licenceScheduleDetailService;
  }

  List<LicenceJson> searchLicencesWithSchedulesByReferenceTypeAndStatus(
      String searchTerm,
      List<LicenceType> types,
      LicenceScheduleDetailStatus status
  ) {
    return licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
          searchTerm,
          types,
          status
        ).stream()
        .map(LicenceScheduleDetail::getLicenceSchedule)
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
