package uk.co.nstauthority.licensingmanagementservice.caseevent;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;

public record CaseEventUser(@Nullable Long userWuaId, @Nullable Long proxyWuaId, boolean isSystem) {

  static CaseEventUser from(ServiceUserDetail user) {
    return new CaseEventUser(user.wuaId(), user.proxyWuaId(), false);
  }

  static CaseEventUser system() {
    return new CaseEventUser(null, null, true);
  }
}
