package uk.co.nstauthority.template.authentication.saml;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.authentication.ServiceUserDetailTestUtil;

import java.util.Collection;
import java.util.HashSet;

public class SamlAuthenticationUtil {

  private SamlAuthenticationUtil() {}

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Builder() {
    }

    private ServiceUserDetail serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    private final Collection<GrantedAuthority> grantedAuthorities = new HashSet<>();

    public void setSecurityContext() {
      var authentication = build();
      SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    public ServiceSaml2Authentication build() {
      return new ServiceSaml2Authentication(serviceUserDetail, grantedAuthorities);
    }

    public Builder withGrantedAuthorities(Collection<GrantedAuthority> grantedAuthorities) {
      this.grantedAuthorities.addAll(grantedAuthorities);
      return this;
    }

    public Builder withGrantedAuthority(GrantedAuthority grantedAuthority) {
      this.grantedAuthorities.add(grantedAuthority);
      return this;
    }

    public Builder withGrantedAuthority(String grantedAuthority) {
      this.grantedAuthorities.add(new SimpleGrantedAuthority(grantedAuthority));
      return this;
    }

    public Builder withUser(ServiceUserDetail serviceUserDetail) {
      this.serviceUserDetail = serviceUserDetail;
      return this;
    }
  }
}
