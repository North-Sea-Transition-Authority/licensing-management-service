package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.energyportalapi.generated.types.Address;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;

@Service
public class DocumentLinkingService {

  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final LicenceContinuationService licenceContinuationService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  public DocumentLinkingService(
      OrganisationUnitQueryService organisationUnitQueryService,
      LicenceContinuationService licenceContinuationService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.licenceContinuationService = licenceContinuationService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
  }

  public String getApplicationCompanyNameFromDto(DocumentInstanceDto documentInstanceDto) {
    var responsibleOrganisationUnitId = getApplicationResponsibleOrganisationUnitId(documentInstanceDto);

    return organisationUnitQueryService.getOrganisationUnitNameById(responsibleOrganisationUnitId)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Could not find the name for organisation unit %d".formatted(responsibleOrganisationUnitId)
        ));
  }

  public Address getApplicationCompanyAddressFromDto(DocumentInstanceDto documentInstanceDto) {
    var responsibleOrganisationUnitId = getApplicationResponsibleOrganisationUnitId(documentInstanceDto);

    return organisationUnitQueryService.getOrganisationUnitAddressById(responsibleOrganisationUnitId)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Could not find the address for organisation unit %d".formatted(responsibleOrganisationUnitId)
        ));
  }

  private Integer getApplicationResponsibleOrganisationUnitId(DocumentInstanceDto documentInstanceDto) {
    var applicationId = UUID.fromString(documentInstanceDto.itemReference());
    var applicationType = ApplicationType.valueOf(documentInstanceDto.itemType());

    return switch (applicationType) {
      case CONTINUATION_APPLICATION -> licenceContinuationService.getDetailByIdOrThrow(applicationId)
          .getResponsibleOrganisationUnitId();
      case SCHEDULE_AMENDMENT_APPLICATION ->  scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationId)
          .getResponsibleOrganisationUnitId();
    };
  }
}
