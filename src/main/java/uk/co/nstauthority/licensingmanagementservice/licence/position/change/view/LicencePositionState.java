package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record LicencePositionState(
    @Nullable Integer administratorId,
    List<Integer> licenseeIds,
    Map<Integer, BigDecimal> equityByOrganisationId
) {

  public static final LicencePositionState EMPTY = new LicencePositionState(null, List.of(), Map.of());

  public LicencePositionState withAdministratorId(Integer administratorId) {
    return new LicencePositionState(administratorId, licenseeIds, equityByOrganisationId);
  }

  public LicencePositionState withEquityByOrganisationId(Map<Integer, BigDecimal> equityByOrganisationId) {
    return new LicencePositionState(administratorId, licenseeIds, equityByOrganisationId);
  }

  public LicencePositionState withLicenseeIds(List<Integer> licenseesToAdd, List<Integer> licenseesToRemove) {
    var mutableList = new ArrayList<>(licenseeIds);
    mutableList.addAll(licenseesToAdd);
    mutableList.removeAll(licenseesToRemove);
    return new LicencePositionState(administratorId, mutableList, equityByOrganisationId);
  }
}