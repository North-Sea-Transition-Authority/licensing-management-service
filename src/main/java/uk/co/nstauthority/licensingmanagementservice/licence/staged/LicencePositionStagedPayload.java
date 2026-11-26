package uk.co.nstauthority.licensingmanagementservice.licence.staged;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEntity;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.LicencePositionPayload;

@Audited
@Entity(name = "licence_position_staged_payloads")
@PathVariableEntity(pathVariableName = "licencePositionStagedPayloadId")
public class LicencePositionStagedPayload {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_correction_id")
  private LicenceCorrection licenceCorrection;

  @Enumerated(EnumType.STRING)
  private LicencePositionStagedPayloadChangeType changeType;

  @ManyToOne
  @JoinColumn(name = "target_licence_position_id")
  private LicencePosition targetLicencePosition;

  @JdbcTypeCode(SqlTypes.JSON)
  private LicencePositionPayload payload;

  public LicencePositionStagedPayload() {

  }

  LicencePositionStagedPayload(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  /**
   * The position this staged payload acts on, whether that is a position it adds or an executed one it targets.
   */
  public UUID getPositionId() {
    if (payload instanceof CreateLicencePositionPayload create) {
      return UUID.fromString(create.licencePositionId());
    }
    return targetLicencePosition.getId();
  }

  public LicenceCorrection getLicenceCorrection() {
    return licenceCorrection;
  }

  public void setLicenceCorrection(LicenceCorrection licenceCorrection) {
    this.licenceCorrection = licenceCorrection;
  }

  public LicencePositionStagedPayloadChangeType getChangeType() {
    return changeType;
  }

  public void setChangeType(LicencePositionStagedPayloadChangeType changeType) {
    this.changeType = changeType;
  }

  public LicencePosition getTargetLicencePosition() {
    return targetLicencePosition;
  }

  public void setTargetLicencePosition(LicencePosition targetLicencePosition) {
    this.targetLicencePosition = targetLicencePosition;
  }

  public LicencePositionPayload getPayload() {
    return payload;
  }

  public void setPayload(LicencePositionPayload payload) {
    this.payload = payload;
  }
}