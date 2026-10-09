package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.util.Map;
import java.util.Set;

public class PositionStateTestUtil {

  private Integer administratorId = null;

  public static PositionStateTestUtil newBuilder() {
    return new PositionStateTestUtil();
  }

  public PositionStateTestUtil withAdministratorId(Integer administratorId) {
    this.administratorId = administratorId;
    return this;
  }

  public LicencePositionState build() {
    return new LicencePositionState(administratorId, Set.of(), Map.of(), Map.of());
  }
}
