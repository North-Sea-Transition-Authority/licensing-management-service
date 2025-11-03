package uk.co.nstauthority.licensingmanagementservice.audit;

import java.util.Optional;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.saml.ServiceSaml2Authentication;

@Service
public class AuditRevisionListener implements RevisionListener {

  @Override
  public void newRevision(Object revision) {
    var auditRevision = (AuditRevision) revision;
    getUserDetail().ifPresent(user -> {
          auditRevision.setUserWuaId(user.wuaId());
          auditRevision.setProxyUserWuaId(user.proxyWuaId());
        }
    );
  }

  // We use this method and not UserDetailService#getUserDetail as that method throws an exception if there is no user
  // logged in and there are scenarios when audit revisions are created when there is no user logged in.
  // Examples are:
  // - When the initial fee period / fee lines are created in our ApplicationReadyEvent listener.
  // - When the payment reconcile job in the payments library updates a payment.
  // This method is located here and not in the UserDetailService as there are no other places that should be calling
  // getUserDetail when there is no user logged in.
  private Optional<ServiceUserDetail> getUserDetail() {
    if (SecurityContextHolder.getContext().getAuthentication() instanceof ServiceSaml2Authentication authentication
        && authentication.getPrincipal() instanceof ServiceUserDetail serviceUserDetail) {
      return Optional.of(serviceUserDetail);
    }

    return Optional.ofNullable(AuditRevisionUtil.getFallbackAuditUser());
  }
}
