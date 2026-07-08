package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

public class CreateLicencePositionPayloadTestUtil {

  private String licencePositionId = UUID.randomUUID().toString();
  private String licenceTransactionId = UUID.randomUUID().toString();
  private LocalDate effectiveDate = LocalDate.of(2026, Month.JUNE, 5);
  private Integer effectiveDateOrder = 1;
  private String correctionReference = "CORRECTION-REF";
  private List<LicencePositionChangeType> changes = List.of();

  public static CreateLicencePositionPayloadTestUtil newBuilder() {
    return new CreateLicencePositionPayloadTestUtil();
  }

  public CreateLicencePositionPayloadTestUtil withLicencePositionId(String licencePositionId) {
    this.licencePositionId = licencePositionId;
    return this;
  }

  public CreateLicencePositionPayloadTestUtil withLicenceTransactionId(String licenceTransactionId) {
    this.licenceTransactionId = licenceTransactionId;
    return this;
  }

  public CreateLicencePositionPayloadTestUtil withEffectiveDate(LocalDate effectiveDate) {
    this.effectiveDate = effectiveDate;
    return this;
  }

  public CreateLicencePositionPayloadTestUtil withEffectiveDateOrder(Integer effectiveDateOrder) {
    this.effectiveDateOrder = effectiveDateOrder;
    return this;
  }

  public CreateLicencePositionPayloadTestUtil withCorrectionReference(String correctionReference) {
    this.correctionReference = correctionReference;
    return this;
  }

  public CreateLicencePositionPayloadTestUtil withChanges(List<LicencePositionChangeType> changes) {
    this.changes = changes;
    return this;
  }

  public CreateLicencePositionPayload build() {
    return LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(licencePositionId)
        .withLicenceTransactionId(licenceTransactionId)
        .withEffectiveDate(effectiveDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .withCorrectionReference(correctionReference)
        .withChanges(changes)
        .build();
  }
}