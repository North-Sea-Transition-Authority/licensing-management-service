package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.util.Map;

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
    return new LicencePositionState(administratorId, Map.of());
  }
}
