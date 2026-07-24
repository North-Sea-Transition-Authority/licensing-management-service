package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

@Service
public class LicencePositionChangeViewService {

  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicencePositionChangeViewService(
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public Map<String, LicencePositionChangeView> getChangeViews(
      UUID currentPositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    return chronologicalPositions.stream()
        .filter(chronologicalPosition -> chronologicalPosition.id().equals(currentPositionId))
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .flatMap(change -> change.operations().stream()
            .map(operation -> Map.entry(
                operation.type(),
                toView(operation, change, currentPositionId, chronologicalPositions))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private LicencePositionChangeView toView(
      LicenceOperation operation,
      PositionChange change,
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    return switch (operation) {
      case AdministratorOperation administratorChange ->
          buildAdministratorChange(
              administratorChange,
              change,
              currentLicencePositionId,
              chronologicalPositions
          );
    };
  }

  private AdministratorChangeView buildAdministratorChange(
      AdministratorOperation operation,
      PositionChange change,
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions
  ) {
    var joiningId = operation.operatorId();

    var administratorIdChangeByPositionId = LicencePositionChangeUtil.administratorIdChangeByPositionId(chronologicalPositions);

    Integer withdrawingId = null;
    for (var chronologicalPosition : chronologicalPositions) {
      if (chronologicalPosition.id().equals(currentLicencePositionId)) {
        break;
      }
      var operatorId = administratorIdChangeByPositionId.get(chronologicalPosition.id());
      if (operatorId != null) {
        withdrawingId = operatorId;
      }
    }

    var idsToResolve = (withdrawingId == null) ? List.of(joiningId) : List.of(joiningId, withdrawingId);

    var organisationNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(idsToResolve);

    var withdrawingName = (withdrawingId == null) ? null : organisationNames.get(withdrawingId);

    return new AdministratorChangeView(
        withdrawingName, organisationNames.get(joiningId), change.changeId(), change.changeType());
  }
}
