package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;

@Component
class AdministratorChangeFormHandler extends OrganisationChangeFormHandler<AdministratorChangeForm> {

  private final AdministratorChangeService administratorChangeService;

  AdministratorChangeFormHandler(
      OrganisationChangeService organisationChangeService,
      OrganisationChangeFormValidator organisationChangeFormValidator,
      OrganisationUnitQueryService organisationUnitQueryService,
      AdministratorChangeService administratorChangeService
  ) {
    super(organisationChangeService, organisationChangeFormValidator, organisationUnitQueryService);
    this.administratorChangeService = administratorChangeService;
  }

  @Override
  public AddPositionChangeType changeType() {
    return AddPositionChangeType.ADMINISTRATOR;
  }

  @Override
  public Class<? extends VisibleLicenceOperation> operationType() {
    return AdministratorOperation.class;
  }

  @Override
  public String displayName() {
    return "Licence administrator change";
  }

  @Override
  public AdministratorChangeForm newForm() {
    return new AdministratorChangeForm();
  }

  @Override
  protected String operatorType() {
    return "licence administrator";
  }

  @Override
  protected LicenceOperation operation(Integer organisationId) {
    return LicenceOperation.newAdministratorChange()
        .withOperator(organisationId)
        .build();
  }

  @Override
  protected void correctLiveChange(ChangeFormTarget.LiveChange liveChange, Integer organisationId) {
    administratorChangeService.correctExistingAdministratorChange(
        liveChange.position(),
        liveChange.correction(),
        liveChange.change().getId().toString(),
        organisationId
    );
  }
}
