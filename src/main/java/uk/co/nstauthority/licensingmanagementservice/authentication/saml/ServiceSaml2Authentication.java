package uk.co.nstauthority.licensingmanagementservice.authentication.saml;

import java.util.Collection;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;

public class ServiceSaml2Authentication extends AbstractAuthenticationToken {

  private final ServiceUserDetail principal;

  public ServiceSaml2Authentication(ServiceUserDetail principal, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ServiceSaml2Authentication that = (ServiceSaml2Authentication) o;
    return principal.equals(that.principal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), principal);
  }
}
