package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import java.time.Instant;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;

public class LicenceCorrectionTestUtil {

  private UUID id = UUID.randomUUID();
  private Licence licence = LicenceTestUtil.builder().build();
  private String correctionReference = "CORRECTION_REFERENCE";
  private String reason = "a reason";
  private LicenceCorrectionStatus status = LicenceCorrectionStatus.IN_PROGRESS;
  private Long allocatedToWuaId = 0L;
  private Instant createdInstant = Instant.parse("2026-06-05T10:00:00Z");

  public static LicenceCorrectionTestUtil newBuilder() {
    return new LicenceCorrectionTestUtil();
  }

  public LicenceCorrectionTestUtil withId(UUID id) {
    this.id = id;
    return this;
  }

  public LicenceCorrectionTestUtil withLicence(Licence licence) {
    this.licence = licence;
    return this;
  }

  public LicenceCorrectionTestUtil withCorrectionReference(String correctionReference) {
    this.correctionReference = correctionReference;
    return this;
  }

  public LicenceCorrectionTestUtil withReason(String reason) {
    this.reason = reason;
    return this;
  }

  public LicenceCorrectionTestUtil withStatus(LicenceCorrectionStatus status) {
    this.status = status;
    return this;
  }

  public LicenceCorrectionTestUtil withAllocatedToWuaId(Long allocatedToWuaId) {
    this.allocatedToWuaId = allocatedToWuaId;
    return this;
  }

  public LicenceCorrectionTestUtil withCreatedInstant(Instant createdInstant) {
    this.createdInstant = createdInstant;
    return this;
  }

  public LicenceCorrection build() {
    var licenceCorrection = new LicenceCorrection(id);
    licenceCorrection.setLicence(licence);
    licenceCorrection.setCorrectionReference(correctionReference);
    licenceCorrection.setReason(reason);
    licenceCorrection.setStatus(status);
    licenceCorrection.setAllocatedToWuaId(allocatedToWuaId);
    licenceCorrection.setCreatedInstant(createdInstant);

    return licenceCorrection;
  }
}
