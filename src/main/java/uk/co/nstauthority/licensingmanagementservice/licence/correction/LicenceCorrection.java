package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Audited
@Entity(name = "licence_corrections")
public class LicenceCorrection {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_id")
  private Licence licence;

  private String correctionReference;

  private String reason;

  @Enumerated(EnumType.STRING)
  private LicenceCorrectionStatus status;

  private Long allocatedToWuaId;

  private Instant createdInstant;

  public LicenceCorrection() {

  }

  LicenceCorrection(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public Licence getLicence() {
    return licence;
  }

  public void setLicence(Licence licence) {
    this.licence = licence;
  }

  public String getCorrectionReference() {
    return correctionReference;
  }

  public void setCorrectionReference(String correctionReference) {
    this.correctionReference = correctionReference;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public LicenceCorrectionStatus getStatus() {
    return status;
  }

  public void setStatus(LicenceCorrectionStatus status) {
    this.status = status;
  }

  public Long getAllocatedToWuaId() {
    return allocatedToWuaId;
  }

  public void setAllocatedToWuaId(Long allocatedToWuaId) {
    this.allocatedToWuaId = allocatedToWuaId;
  }

  public Instant getCreatedInstant() {
    return createdInstant;
  }

  public void setCreatedInstant(Instant createdInstant) {
    this.createdInstant = createdInstant;
  }
}
