package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionAdministratorChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

@Service
public class LicencePositionChangeViewService {

  public Map<String, LicencePositionChangeView> getChangeViews(
      UUID currentPositionId,
      List<ChronologicalPosition> chronologicalPositions,
      Map<Integer, String> organisationNames
  ) {
    return chronologicalPositions.stream()
        .filter(chronologicalPosition -> chronologicalPosition.id().equals(currentPositionId))
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .sorted(Comparator.comparingLong(PositionChange::changeOrder))
        .flatMap(change -> change.operations().stream()
            .map(operation -> Map.entry(
                operation.type(),
                toView(operation, change, currentPositionId, chronologicalPositions, organisationNames))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private LicencePositionChangeView toView(
      LicenceOperation operation,
      PositionChange change,
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions,
      Map<Integer, String> organisationNames
  ) {
    return switch (operation) {
      case AdministratorOperation administratorChange ->
          buildAdministratorChange(
              administratorChange,
              change,
              currentLicencePositionId,
              chronologicalPositions,
              organisationNames
          );
    };
  }

  private AdministratorChangeView buildAdministratorChange(
      AdministratorOperation operation,
      PositionChange change,
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions,
      Map<Integer, String> organisationNames
  ) {
    var joiningId = operation.operatorId();

    var withdrawingId = LicencePositionAdministratorChangeUtil.resolvePreviousAdministratorId(
        currentLicencePositionId, chronologicalPositions);

    var withdrawingName = (withdrawingId == null) ? null : organisationNames.get(withdrawingId);

    return new AdministratorChangeView(
        withdrawingName, organisationNames.get(joiningId), change.changeId(), change.changeType());
  }
}
