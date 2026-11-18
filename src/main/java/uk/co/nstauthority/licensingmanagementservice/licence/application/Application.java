package uk.co.nstauthority.licensingmanagementservice.licence.application;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Audited
@Entity(name = "applications")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "application_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Application {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_id")
  private Licence licence;

  @Column
  private String applicationReference;

  @Column
  private String withdrawalReason;

  @Column
  private Instant withdrawnDatetime;

  public abstract ApplicationType getApplicationType();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Licence getLicence() {
    return licence;
  }

  public void setLicence(Licence licence) {
    this.licence = licence;
  }

  public String getApplicationReference() {
    return applicationReference;
  }

  public void setApplicationReference(String applicationReference) {
    this.applicationReference = applicationReference;
  }

  public String getWithdrawalReason() {
    return withdrawalReason;
  }

  public void setWithdrawalReason(String withdrawalReason) {
    this.withdrawalReason = withdrawalReason;
  }

  public Instant getWithdrawnDatetime() {
    return withdrawnDatetime;
  }

  public void setWithdrawnDatetime(Instant withdrawnDatetime) {
    this.withdrawnDatetime = withdrawnDatetime;
  }
}
