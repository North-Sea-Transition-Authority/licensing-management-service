package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;

public final class LicencePositionChangeUtil {

  private LicencePositionChangeUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static Map<UUID, Integer> administratorIdChangeByPositionId(List<LicencePositionChange> licencePositionChanges) {
    return licencePositionChanges.stream()
        .flatMap(licencePositionChange -> licencePositionChange.getOperations().stream()
        .filter(AdministratorOperation.class::isInstance)
        .map(adminChange -> Map.entry(
            licencePositionChange.getLicencePosition().getId(),
            ((AdministratorOperation) adminChange).operatorId()
        ))
    )
    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
