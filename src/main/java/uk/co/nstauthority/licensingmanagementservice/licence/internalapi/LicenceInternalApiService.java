package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
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
  private final LicenceService licenceService;

  public LicenceInternalApiService(
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      ApplicationAccessService applicationAccessService,
      LicenceService licenceService
  ) {
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.applicationAccessService = applicationAccessService;
    this.licenceService = licenceService;
  }

  List<LicenceJson> searchLicencesByReferenceAndType(
      String searchTerm,
      List<LicenceType> types
  ) {
    return licenceService.searchLicencesByReferenceAndTypes(searchTerm, types)
        .stream()
        .map(this::toLicenceJson)
        .toList();
  }

  List<LicenceJson> searchLicencesWithInProgressSchedulesByReferenceTypeAndStatus(
      String searchTerm,
      List<LicenceType> types,
      LicenceScheduleDetailStatus status,
      ServiceUserDetail serviceUserDetail
  ) {
    var usersOrganisationUnitIds = applicationAccessService.getOrganisationUnitIds(serviceUserDetail);

    var licenceScheduleDetails = licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        searchTerm,
        types,
        status
    );

    var licences = licenceScheduleDetails.stream()
        .map(LicenceScheduleDetail::getLicenceSchedule)
        .map(LicenceSchedule::getLicence)
        .toList();

    var organisationByLicence = licenceResponsibleOrganisationService.getAllByLicenceIn(licences).stream()
        .collect(Collectors.groupingBy(LicenceResponsibleOrganisation::getLicence));

    return licences.stream()
        .filter(licence -> isUserInLicenseeOrganisation(licence, organisationByLicence, usersOrganisationUnitIds))
        .sorted(Comparator.comparing(Licence::getPrefix).thenComparing(Licence::getLicenceNumber))
        .map(this::toLicenceJson)
        .toList();
  }

  private boolean isUserInLicenseeOrganisation(
      Licence licence,
      Map<Licence, List<LicenceResponsibleOrganisation>> organisationByLicence,
      Set<Integer> usersOrganisationUnitIds
  ) {
    var organisations = organisationByLicence.getOrDefault(licence, Collections.emptyList());

    return organisations.stream()
        .anyMatch(org -> usersOrganisationUnitIds.contains(org.getResponsibleOrganisationId()));
  }

  private LicenceJson toLicenceJson(Licence licence) {
    return new LicenceJson(
        licence.getId(),
        licence.getLicenceReference()
    );
  }
}
