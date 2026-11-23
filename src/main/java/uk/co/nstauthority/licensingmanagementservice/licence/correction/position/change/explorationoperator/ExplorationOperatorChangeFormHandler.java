package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.explorationoperator;

import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.ExplorationOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;

@Component
class ExplorationOperatorChangeFormHandler extends OrganisationChangeFormHandler<ExplorationOperatorChangeForm> {

  ExplorationOperatorChangeFormHandler(
      OrganisationChangeService organisationChangeService,
      OrganisationChangeFormValidator organisationChangeFormValidator,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    super(organisationChangeService, organisationChangeFormValidator, organisationUnitQueryService);
  }

  @Override
  public AddPositionChangeType changeType() {
    return AddPositionChangeType.EXPLORATION_OPERATOR;
  }

  @Override
  public Class<? extends VisibleLicenceOperation> operationType() {
    return ExplorationOperatorOperation.class;
  }

  @Override
  public String displayName() {
    return "Exploration operator change";
  }

  @Override
  public ExplorationOperatorChangeForm newForm() {
    return new ExplorationOperatorChangeForm();
  }

  @Override
  protected String operatorType() {
    return "exploration operator";
  }

  @Override
  protected LicenceOperation operation(Integer organisationId) {
    return LicenceOperation.newExplorationOperatorOperation()
        .withOperator(organisationId)
        .build();
  }
}
