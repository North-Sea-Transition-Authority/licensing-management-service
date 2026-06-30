package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.operations.LicencePositionAdministratorChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.operations.LicencePositionChangeOperation;

@Service
public class LicencePositionChangeViewService {

  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionChangeViewService(
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public Map<String, LicencePositionChangeView> getChangeViews(
      LicencePosition currentLicencePosition,
      List<LicencePosition> chronologicalLicencePositions,
      List<LicencePositionChange> licencePositionChanges
  ) {
    return licencePositionChanges.stream()
        .filter(
            licencePositionChange -> licencePositionChange.getLicencePosition().getId().equals(currentLicencePosition.getId())
        )
        .flatMap(licencePositionChange -> licencePositionChange.getOperations().stream())
        .collect(Collectors.toMap(
            LicencePositionChangeOperation::type,
            operation -> toView(
                operation,
                currentLicencePosition,
                chronologicalLicencePositions,
                licencePositionChanges
            )
        ));
  }

  private LicencePositionChangeView toView(
      LicencePositionChangeOperation operation,
      LicencePosition currentLicencePosition,
      List<LicencePosition> chronologicalLicencePositions,
      List<LicencePositionChange> licencePositionChanges
  ) {
    return switch (operation) {
      case LicencePositionAdministratorChange administratorChange ->
          buildAdministratorChange(
              administratorChange,
              currentLicencePosition,
              chronologicalLicencePositions,
              licencePositionChanges
          );
    };
  }

  private AdministratorChangeView buildAdministratorChange(
      LicencePositionAdministratorChange operation,
      LicencePosition currentLicencePosition,
      List<LicencePosition> chronologicalLicencePositions,
      List<LicencePositionChange> licencePositionChanges
  ) {
    var joiningId = operation.operatorId();

    var administratorIdChangeByPositionId = LicencePositionChangeUtil.administratorIdChangeByPositionId(licencePositionChanges);

    Integer withdrawingId = null;
    for (var licencePosition : chronologicalLicencePositions) {
      if (licencePosition.getId().equals(currentLicencePosition.getId())) {
        break;
      }
      var operatorId = administratorIdChangeByPositionId.get(licencePosition.getId());
      if (operatorId != null) {
        withdrawingId = operatorId;
      }
    }

    var idsToResolve = (withdrawingId == null) ? List.of(joiningId) : List.of(joiningId, withdrawingId);

    var organisationNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(idsToResolve);

    var withdrawingName = (withdrawingId == null) ? null : organisationNames.get(withdrawingId);

    return new AdministratorChangeView(withdrawingName, organisationNames.get(joiningId));
  }
}
