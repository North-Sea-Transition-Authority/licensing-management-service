package uk.co.nstauthority.licensingmanagementservice.licence.transaction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.hibernate.AssignedOrGeneratedUuid;

@Audited
@Entity(name = "licence_transactions")
public class LicenceTransaction {

  @Id
  @AssignedOrGeneratedUuid
  private UUID id;

  private String regulatorReference;

  public LicenceTransaction() {

  }

  public LicenceTransaction(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getRegulatorReference() {
    return regulatorReference;
  }

  public void setRegulatorReference(String regulatorReference) {
    this.regulatorReference = regulatorReference;
  }
}