package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStorageOperatorChangeUtil;

@Component
class StorageOperatorChangeFormHandler extends OrganisationChangeFormHandler<StorageOperatorChangeForm> {

  StorageOperatorChangeFormHandler(
      OrganisationChangeService organisationChangeService,
      OrganisationChangeFormValidator organisationChangeFormValidator,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    super(organisationChangeService, organisationChangeFormValidator, organisationUnitQueryService);
  }

  @Override
  public AddPositionChangeType changeType() {
    return AddPositionChangeType.STORAGE_OPERATOR;
  }

  @Override
  public Class<? extends VisibleLicenceOperation> operationType() {
    return StorageOperatorOperation.class;
  }

  @Override
  public String displayName() {
    return "Storage operator change";
  }

  @Override
  public StorageOperatorChangeForm newForm() {
    return new StorageOperatorChangeForm();
  }

  @Override
  protected String operatorType() {
    return "storage operator";
  }

  @Override
  protected LicenceOperation operation(Integer organisationId) {
    return LicencePositionStorageOperatorChangeUtil.storageOperatorOperation(organisationId);
  }
}
