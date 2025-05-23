package uk.co.nstauthority.licensingmanagementservice.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "audit_revisions")
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevision {

  @Id
  @RevisionNumber
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rev")
  private Long id;

  @RevisionTimestamp
  private Date createdDateTime;

  private Long userWuaId;

  private Long proxyUserWuaId;

  public Long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Long getUserWuaId() {
    return userWuaId;
  }

  public void setUserWuaId(long webUserAccountId) {
    this.userWuaId = webUserAccountId;
  }

  public Long getProxyUserWuaId() {
    return proxyUserWuaId;
  }

  public void setProxyUserWuaId(Long proxyUserWuaId) {
    this.proxyUserWuaId = proxyUserWuaId;
  }

  public Date getCreatedDateTime() {
    return createdDateTime;
  }

  public void setCreatedDateTime(Date createdDateTime) {
    this.createdDateTime = createdDateTime;
  }
}
