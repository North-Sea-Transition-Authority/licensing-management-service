package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public record LicenseeChangeContext(
    List<Integer> currentJoiningLicenseeIds,
    List<Integer> currentWithdrawingLicenseeIds,
    List<Integer> previousLicenseeIds,
    List<Integer> currentLicenseeIds,
    Map<Integer, String> organisationNames
) {
  public List<String> currentJoiningLicenseeNames() {
    return getNamesFromIds(currentJoiningLicenseeIds, organisationNames);
  }

  public List<String> currentWithdrawingLicenseeNames() {
    return getNamesFromIds(currentWithdrawingLicenseeIds, organisationNames);
  }

  public List<String> previousLicenseeNames() {
    return getNamesFromIds(previousLicenseeIds, organisationNames);
  }

  public List<String> currentLicenseeNames() {
    return getNamesFromIds(currentLicenseeIds, organisationNames);
  }

  private List<String> getNamesFromIds(Collection<Integer> ids, Map<Integer, String> organisationNames) {
    return ids
        .stream()
        .map(id -> nameOrEmpty(organisationNames, id))
        .sorted()
        .toList();
  }

  private static String nameOrEmpty(Map<Integer, String> licenseeNames, @Nullable Integer licenseeId) {
    return licenseeId == null ? "" : licenseeNames.getOrDefault(licenseeId, "");
  }
}
