package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@Service
public class LicenceInternalApiService {

  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final ApplicationAccessService applicationAccessService;

  public LicenceInternalApiService(
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      ApplicationAccessService applicationAccessService
  ) {

    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.applicationAccessService = applicationAccessService;
  }

  List<LicenceJson> searchLicencesWithSchedulesByReferenceTypeAndStatus(
      String searchTerm,
      List<LicenceType> types,
      LicenceScheduleDetailStatus status,
      ServiceUserDetail serviceUserDetail
  ) {

    var authorisedUnitIds = applicationAccessService.getOrganisationUnitIds(serviceUserDetail);

    return licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
          searchTerm,
          types,
          status
        ).stream()
        .map(LicenceScheduleDetail::getLicenceSchedule)
        .map(LicenceSchedule::getLicence)
        .filter(licence -> isUserInLicenseeOrganisation(licence, authorisedUnitIds))
        .sorted(Comparator.comparing(Licence::getPrefix).thenComparing(Licence::getLicenceNumber))
        .map(this::toLicenceJson)
        .toList();
  }

  private boolean isUserInLicenseeOrganisation(
      Licence licence,
      Set<Integer> authorisedUnitIds
  ) {

    return licenceResponsibleOrganisationService
        .getAllByLicence(licence)
        .stream()
        .anyMatch(
            responsibleOrganisationId -> authorisedUnitIds.contains(responsibleOrganisationId.getResponsibleOrganisationId()));
  }

  private LicenceJson toLicenceJson(Licence licence) {
    return new LicenceJson(
        licence.getId(),
        licence.getLicenceReference()
    );
  }
}
