package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

public class LicencePositionStagedPayloadTestUtil {

  private UUID id = UUID.randomUUID();
  private LicenceCorrection licenceCorrection = LicenceCorrectionTestUtil.newBuilder().build();
  private LicencePositionStagedPayloadChangeType changeType = LicencePositionStagedPayloadChangeType.ADD_POSITION;
  private LicencePosition targetLicencePosition = LicencePositionTestUtil.newBuilder().build();
  private LicencePositionPayload payload = LicencePositionPayload.newCreateLicencePositionPayload()
      .withLicencePositionId(UUID.randomUUID().toString())
      .build();

  public static LicencePositionStagedPayloadTestUtil newBuilder() {
    return new LicencePositionStagedPayloadTestUtil();
  }

  public LicencePositionStagedPayloadTestUtil withId(UUID id) {
    this.id = id;
    return this;
  }

  public LicencePositionStagedPayloadTestUtil withLicenceCorrection(LicenceCorrection licenceCorrection) {
    this.licenceCorrection = licenceCorrection;
    return this;
  }

  public LicencePositionStagedPayloadTestUtil withChangeType(LicencePositionStagedPayloadChangeType changeType) {
    this.changeType = changeType;
    return this;
  }

  public LicencePositionStagedPayloadTestUtil withTargetLicencePosition(LicencePosition targetLicencePosition) {
    this.targetLicencePosition = targetLicencePosition;
    return this;
  }

  public LicencePositionStagedPayloadTestUtil withPayload(LicencePositionPayload payload) {
    this.payload = payload;
    return this;
  }

  public LicencePositionStagedPayload build() {
    var licencePositionStagedPayload = new LicencePositionStagedPayload(id);
    licencePositionStagedPayload.setLicenceCorrection(licenceCorrection);
    licencePositionStagedPayload.setChangeType(changeType);
    licencePositionStagedPayload.setTargetLicencePosition(targetLicencePosition);
    licencePositionStagedPayload.setPayload(payload);

    return licencePositionStagedPayload;
  }
}