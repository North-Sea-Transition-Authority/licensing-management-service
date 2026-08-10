package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Map;

public record LicencePositionState(
    @Nullable Integer administratorId,
    Map<Integer, BigDecimal> equityByOrganisationId
) {

  public static final LicencePositionState EMPTY = new LicencePositionState(null, Map.of());

  public LicencePositionState withAdministratorId(Integer administratorId) {
    return new LicencePositionState(administratorId, equityByOrganisationId);
  }

  public LicencePositionState withEquityByOrganisationId(Map<Integer, BigDecimal> equityByOrganisationId) {
    return new LicencePositionState(administratorId, equityByOrganisationId);
  }
}