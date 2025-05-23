package uk.co.nstauthority.licensingmanagementservice.energyportal.user;

import java.io.Serializable;

public record WebUserAccountId(long id) implements Serializable {

  public static WebUserAccountId from(Long userWuaId) {
    return new WebUserAccountId(userWuaId);
  }

  public int toInt() {
    return ((Long) id).intValue();
  }

  @Override
  public String toString() {
    return String.valueOf(id);
  }
}
