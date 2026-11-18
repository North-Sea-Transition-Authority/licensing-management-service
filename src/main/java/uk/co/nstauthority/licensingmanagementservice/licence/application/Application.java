package uk.co.nstauthority.licensingmanagementservice.licence.application;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Audited
@MappedSuperclass
public abstract class Application {

  @Id
  @UuidGenerator
  private UUID id;

  @Column
  private String applicationReference;

  public abstract ApplicationType getApplicationType();

  public abstract Licence getLicence();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getApplicationReference() {
    return applicationReference;
  }

  public void setApplicationReference(String applicationReference) {
    this.applicationReference = applicationReference;
  }
}
