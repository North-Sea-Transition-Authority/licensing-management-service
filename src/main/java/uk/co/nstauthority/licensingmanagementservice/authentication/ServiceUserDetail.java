package uk.co.nstauthority.licensingmanagementservice.authentication;

import java.io.Serializable;
import java.util.Objects;
import org.springframework.security.core.AuthenticatedPrincipal;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;

public record ServiceUserDetail(Long wuaId,
                                Long personId,
                                String forename,
                                String surname,
                                String emailAddress,
                                Long proxyWuaId,
                                String proxyUsername)
    implements AuthenticatedPrincipal, Serializable {

  @Override
  public String getName() {
    // The 'name' is a unique identifier for this principal, it is not related to the users forename/surname
    return Objects.nonNull(proxyWuaId) ? proxyWuaId.toString() : wuaId.toString();
  }

  public String displayName() {
    return "%s %s".formatted(forename, surname);
  }

  public String displayNameAndEmail() {
    return "%s (%s)".formatted(displayName(), emailAddress);
  }

  public String displayNameIncludingAnyProxyUser() {
    var userDisplayName = displayName();
    return proxyWuaId != null ? String.format("%s as %s", proxyUsername, userDisplayName) : userDisplayName;
  }

  public static ServiceUserDetail from(EnergyPortalUserJson energyPortalUser) {
    return new ServiceUserDetail(
        energyPortalUser.webUserAccountId(),
        null,
        energyPortalUser.forename(),
        energyPortalUser.surname(),
        energyPortalUser.emailAddress(),
        null,
        null
    );
  }
}
