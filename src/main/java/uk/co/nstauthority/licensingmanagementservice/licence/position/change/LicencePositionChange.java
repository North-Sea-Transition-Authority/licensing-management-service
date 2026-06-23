package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.operations.LicencePositionChangeOperation;

@Audited
@Entity(name = "licence_position_changes")
public class LicencePositionChange {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  private LicencePosition licencePosition;

  @JdbcTypeCode(SqlTypes.JSON)
  private List<LicencePositionChangeOperation> operations;

  private long changeOrder;

  @Enumerated(EnumType.STRING)
  private LicencePositionChangeStatus status;

  public LicencePositionChange() {

  }

  LicencePositionChange(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public LicencePosition getLicencePosition() {
    return licencePosition;
  }

  public void setLicencePosition(LicencePosition licencePosition) {
    this.licencePosition = licencePosition;
  }

  public List<LicencePositionChangeOperation> getOperations() {
    return operations;
  }

  public void setOperations(List<LicencePositionChangeOperation> operations) {
    this.operations = operations;
  }

  public long getChangeOrder() {
    return changeOrder;
  }

  public void setChangeOrder(long changeOrder) {
    this.changeOrder = changeOrder;
  }

  public LicencePositionChangeStatus getStatus() {
    return status;
  }

  public void setStatus(LicencePositionChangeStatus status) {
    this.status = status;
  }
}
