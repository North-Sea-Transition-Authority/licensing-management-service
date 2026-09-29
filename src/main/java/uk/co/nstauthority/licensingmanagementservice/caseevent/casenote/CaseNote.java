package uk.co.nstauthority.licensingmanagementservice.caseevent.casenote;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@Audited
@Entity
@Table(name = "case_notes")
public class CaseNote {

  @Id
  @UuidGenerator
  private UUID id;

  @Enumerated(EnumType.STRING)
  private ApplicationType applicationType;

  private UUID applicationId;

  private String noteText;

  private Instant createdInstant;

  private Long authorWuaId;

  private Long proxyWuaId;

  protected CaseNote() {
  }

  @VisibleForTesting
  CaseNote(UUID id) {
    this.id = id;
  }

  CaseNote(
      ApplicationType applicationType,
      UUID applicationId,
      String noteText,
      Instant createdInstant,
      Long authorWuaId,
      Long proxyWuaId
  ) {
    this.applicationType = applicationType;
    this.applicationId = applicationId;
    this.noteText = noteText;
    this.createdInstant = createdInstant;
    this.authorWuaId = authorWuaId;
    this.proxyWuaId = proxyWuaId;
  }

  public UUID getId() {
    return id;
  }

  public ApplicationType getApplicationType() {
    return applicationType;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public String getNoteText() {
    return noteText;
  }

  public Instant getCreatedInstant() {
    return createdInstant;
  }

  public Long getAuthorWuaId() {
    return authorWuaId;
  }

  public Long getProxyWuaId() {
    return proxyWuaId;
  }
}
