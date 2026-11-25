package uk.co.nstauthority.licensingmanagementservice.caseevent;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseEventPayload;

@Audited
@Entity
@Table(name = "case_events")
public class CaseEvent {

  @Id
  @UuidGenerator
  private UUID id;

  private UUID applicationId;

  @Enumerated(EnumType.STRING)
  private CaseEventType eventType;

  private Instant eventInstant;

  private Long userWuaId;

  private Long proxyWuaId;

  private boolean isSystem;

  @JdbcTypeCode(SqlTypes.JSON)
  private CaseEventPayload payload;

  protected CaseEvent() {
  }

  CaseEvent(
      UUID applicationId,
      CaseEventType eventType,
      Instant eventInstant,
      CaseEventUser user,
      CaseEventPayload payload
  ) {
    this.applicationId = applicationId;
    this.eventType = eventType;
    this.eventInstant = eventInstant;
    this.userWuaId = user.userWuaId();
    this.proxyWuaId = user.proxyWuaId();
    this.isSystem = user.isSystem();
    this.payload = payload;
  }

  public UUID getId() {
    return id;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public CaseEventType getEventType() {
    return eventType;
  }

  public Instant getEventInstant() {
    return eventInstant;
  }

  public Long getUserWuaId() {
    return userWuaId;
  }

  public Long getProxyWuaId() {
    return proxyWuaId;
  }

  public boolean isSystem() {
    return isSystem;
  }

  public CaseEventPayload getPayload() {
    return payload;
  }
}
