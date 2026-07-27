package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

public class LicencePositionCorrectionTestUtil {

  private UUID id = UUID.randomUUID();
  private LicenceCorrection licenceCorrection = LicenceCorrectionTestUtil.newBuilder().build();
  private LicencePositionCorrectionChangeType changeType = LicencePositionCorrectionChangeType.ADD_POSITION;
  private LicencePosition targetLicencePosition = LicencePositionTestUtil.newBuilder().build();
  private LicencePositionPayload payload = LicencePositionPayload.newCreateLicencePositionPayload().build();

  public static LicencePositionCorrectionTestUtil newBuilder() {
    return new LicencePositionCorrectionTestUtil();
  }

  public LicencePositionCorrectionTestUtil withId(UUID id) {
    this.id = id;
    return this;
  }

  public LicencePositionCorrectionTestUtil withLicenceCorrection(LicenceCorrection licenceCorrection) {
    this.licenceCorrection = licenceCorrection;
    return this;
  }

  public LicencePositionCorrectionTestUtil withChangeType(LicencePositionCorrectionChangeType changeType) {
    this.changeType = changeType;
    return this;
  }

  public LicencePositionCorrectionTestUtil withTargetLicencePosition(LicencePosition targetLicencePosition) {
    this.targetLicencePosition = targetLicencePosition;
    return this;
  }

  public LicencePositionCorrectionTestUtil withPayload(LicencePositionPayload payload) {
    this.payload = payload;
    return this;
  }

  public LicencePositionCorrection build() {
    var licencePositionCorrection = new LicencePositionCorrection(id);
    licencePositionCorrection.setLicenceCorrection(licenceCorrection);
    licencePositionCorrection.setChangeType(changeType);
    licencePositionCorrection.setTargetLicencePosition(targetLicencePosition);
    licencePositionCorrection.setPayload(payload);

    return licencePositionCorrection;
  }
}