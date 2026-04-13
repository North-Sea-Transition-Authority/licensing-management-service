package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.energyportalapi.generated.types.Address;
import uk.co.fivium.formlibrary.validator.date.DateUtils;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@Service
public class DocumentLinkingService {

  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final LicenceContinuationService licenceContinuationService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceScheduleService licenceScheduleService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;

  public DocumentLinkingService(
      OrganisationUnitQueryService organisationUnitQueryService,
      LicenceContinuationService licenceContinuationService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceScheduleService licenceScheduleService,
      LicenceTypeRulesResolver licenceTypeRulesResolver
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.licenceContinuationService = licenceContinuationService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceScheduleService = licenceScheduleService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
  }

  public String getApplicationCompanyNameFromDto(DocumentInstanceDto documentInstanceDto) {
    var responsibleOrganisationUnitId = getApplicationResponsibleOrganisationUnitId(documentInstanceDto);

    return organisationUnitQueryService.getOrganisationUnitNameById(responsibleOrganisationUnitId)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Could not find the name for organisation unit %d".formatted(responsibleOrganisationUnitId)
        ));
  }

  public Optional<Address> getApplicationCompanyAddressFromDto(DocumentInstanceDto documentInstanceDto) {
    var responsibleOrganisationUnitId = getApplicationResponsibleOrganisationUnitId(documentInstanceDto);

    return organisationUnitQueryService.getOrganisationUnitAddressById(responsibleOrganisationUnitId);
  }

  public String getApplicationLicenceReferenceFromDto(DocumentInstanceDto documentInstanceDto) {
    var applicationId = UUID.fromString(documentInstanceDto.itemReference());
    var applicationType = ApplicationType.valueOf(documentInstanceDto.itemType());

    return switch (applicationType) {
      case CONTINUATION_APPLICATION -> licenceContinuationService
          .getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(applicationId)
          .getLicenceContinuationApplication()
          .getLicenceSchedule()
          .getLicence()
          .getLicenceReference();
      case SCHEDULE_AMENDMENT_APPLICATION -> scheduleWorkProgrammeApplicationService
          .getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(applicationId)
          .getScheduleWorkProgrammeApplication()
          .getLicenceSchedule()
          .getLicence()
          .getLicenceReference();
    };
  }

  public String getCurrentTermPhaseNameFromDto(DocumentInstanceDto documentInstanceDto) {
    var applicationId = UUID.fromString(documentInstanceDto.itemReference());
    var applicationType = ApplicationType.valueOf(documentInstanceDto.itemType());

    var scheduleDetail = switch (applicationType) {
      case CONTINUATION_APPLICATION -> getScheduleDetailFromContinuationApplication(applicationId);
      case SCHEDULE_AMENDMENT_APPLICATION -> scheduleWorkProgrammeApplicationService
          .getScheduleDetailFromApplicationDetail(
              scheduleWorkProgrammeApplicationService
                  .getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(applicationId)
          );
    };

    var licence = scheduleDetail.getLicenceSchedule().getLicence();
    var currentTerm = licenceScheduleService.getCurrentTerm(scheduleDetail);

    return resolveCurrentTermOrPhaseName(licence, currentTerm);
  }

  private String resolveCurrentTermOrPhaseName(Licence licence, LicenceScheduleTerm currentTerm) {
    if (licenceTypeRulesResolver.hasPhases(licence.getType())) {
      var currentPhase = licenceScheduleService.getCurrentPhase(currentTerm);
      if (currentPhase != null) {
        return currentPhase.getPhaseType().getDisplayName();
      }
    }

    return currentTerm.getTermType().getDisplayName();
  }

  public String getNextTermPhaseStartDateFromDto(DocumentInstanceDto documentInstanceDto) {
    var applicationId = UUID.fromString(documentInstanceDto.itemReference());
    var scheduleDetail = getScheduleDetailFromContinuationApplication(applicationId);

    return licenceScheduleService.getNextTermPhaseStartDate(scheduleDetail)
        .map(DateUtils::format)
        .orElse("");
  }

  private LicenceScheduleDetail getScheduleDetailFromContinuationApplication(UUID applicationId) {
    return licenceContinuationService.getScheduleDetailFromApplicationDetail(
        licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(applicationId)
    );
  }

  private Integer getApplicationResponsibleOrganisationUnitId(DocumentInstanceDto documentInstanceDto) {
    var applicationId = UUID.fromString(documentInstanceDto.itemReference());
    var applicationType = ApplicationType.valueOf(documentInstanceDto.itemType());

    return switch (applicationType) {
      case CONTINUATION_APPLICATION -> licenceContinuationService
          .getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(applicationId)
          .getResponsibleOrganisationUnitId();
      case SCHEDULE_AMENDMENT_APPLICATION -> scheduleWorkProgrammeApplicationService
          .getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(applicationId)
          .getResponsibleOrganisationUnitId();
    };
  }
}