package uk.co.nstauthority.template.authentication;

import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.template.authentication.saml.ServiceSaml2Authentication;

@Service
public class UserDetailService {

  public ServiceUserDetail getUserDetail() {
    if (SecurityContextHolder.getContext().getAuthentication() instanceof ServiceSaml2Authentication authentication) {
      if (authentication.getPrincipal() instanceof ServiceUserDetail serviceUserDetail) {
        return serviceUserDetail;
      } else {
        throw new InvalidAuthenticationException("ServiceUserDetail not found in ServiceSaml2Authentication principal");
      }
    } else {
      throw new InvalidAuthenticationException("ServiceSaml2Authentication not found in authentication context");
    }
  }

  public Optional<ServiceUserDetail> findUserDetail() {
    try {
      return Optional.of(getUserDetail());
    } catch (InvalidAuthenticationException e) {
      return Optional.empty();
    }
  }

  public boolean isUserLoggedIn() {
    return findUserDetail().isPresent();
  }

}
