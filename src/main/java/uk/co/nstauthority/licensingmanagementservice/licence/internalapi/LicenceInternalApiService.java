package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;

@Service
public class LicenceInternalApiService {

  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final ApplicationAccessService applicationAccessService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceContinuationService licenceContinuationService;

  public LicenceInternalApiService(
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      ApplicationAccessService applicationAccessService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceContinuationService licenceContinuationService
  ) {
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.applicationAccessService = applicationAccessService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceContinuationService = licenceContinuationService;
  }

  List<LicenceJson> searchLicencesWithInProgressSchedulesByReferenceTypeAndStatusForEaaApplication(
      String searchTerm,
      List<LicenceType> types,
      LicenceScheduleDetailStatus status,
      ServiceUserDetail serviceUserDetail
  ) {
    var licencesWithActiveApplications = scheduleWorkProgrammeApplicationService
        .getAllScheduleWorkProgrammeApplicationDetailsByStatuses(Set.of(
            ScheduleWorkProgrammeApplicationStatus.DRAFT,
            ScheduleWorkProgrammeApplicationStatus.SUBMITTED
        )
    ).stream()
        .map(scheduleWorkProgrammeApplicationService::getLicenceFromScheduleWorkProgrammeApplicationDetail)
        .toList();

    return doSearch(
        searchTerm,
        types,
        status,
        serviceUserDetail,
        licencesWithActiveApplications
    );
  }

  List<LicenceJson> searchLicencesWithInProgressSchedulesByReferenceTypeAndStatusForContinuationApplication(
      String searchTerm,
      List<LicenceType> types,
      LicenceScheduleDetailStatus status,
      ServiceUserDetail serviceUserDetail
  ) {
    var licencesWithActiveApplications = licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(Set.of(
        LicenceContinuationApplicationStatus.DRAFT,
        LicenceContinuationApplicationStatus.SUBMITTED
    )).stream()
        .map(licenceContinuationService::getLicenceFromContinuationApplicationDetail)
        .toList();

    return doSearch(
        searchTerm,
        types,
        status,
        serviceUserDetail,
        licencesWithActiveApplications
    );
  }

  private List<LicenceJson> doSearch(
      String searchTerm,
      List<LicenceType> types,
      LicenceScheduleDetailStatus status,
      ServiceUserDetail serviceUserDetail,
      List<Licence> excludedLicences
  ) {
    var usersOrganisationUnitIds = applicationAccessService.getOrganisationUnitIds(serviceUserDetail);

    return licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
            searchTerm,
            types,
            status
        ).stream()
        .map(LicenceScheduleDetail::getLicenceSchedule)
        .map(LicenceSchedule::getLicence)
        .filter(licence -> isUserInLicenseeOrganisation(licence, usersOrganisationUnitIds))
        .filter(licence -> !excludedLicences.contains(licence))
        .sorted(Comparator.comparing(Licence::getPrefix).thenComparing(Licence::getLicenceNumber))
        .map(this::toLicenceJson)
        .toList();
  }

  private boolean isUserInLicenseeOrganisation(
      Licence licence,
      Set<Integer> usersOrganisationUnitIds
  ) {

    return licenceResponsibleOrganisationService
        .getAllByLicence(licence)
        .stream()
        .anyMatch(
            responsibleOrganisationId ->
                usersOrganisationUnitIds.contains(
                    responsibleOrganisationId.getResponsibleOrganisationId()
                )
        );
  }

  private LicenceJson toLicenceJson(Licence licence) {
    return new LicenceJson(
        licence.getId(),
        licence.getLicenceReference()
    );
  }
}
