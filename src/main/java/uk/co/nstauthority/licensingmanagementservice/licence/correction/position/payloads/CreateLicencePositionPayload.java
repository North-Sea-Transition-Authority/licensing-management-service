package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import java.time.LocalDate;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

public record CreateLicencePositionPayload(
    String licencePositionId,
    String licenceTransactionId,
    LocalDate effectiveDate,
    Integer effectiveDateOrder,
    String correctionReference,
    List<LicencePositionChangeType> changes
) implements LicencePositionPayload {

  @Override
  public String type() {
    return ADD_POSITION;
  }

  public static class Builder {

    private String licencePositionId;
    private String licenceTransactionId;
    private LocalDate effectiveDate;
    private Integer effectiveDateOrder;
    private String correctionReference;
    private List<LicencePositionChangeType> changes = List.of();

    public Builder withLicencePositionId(String licencePositionId) {
      this.licencePositionId = licencePositionId;
      return this;
    }

    public Builder withLicenceTransactionId(String licenceTransactionId) {
      this.licenceTransactionId = licenceTransactionId;
      return this;
    }

    public Builder withEffectiveDate(LocalDate effectiveDate) {
      this.effectiveDate = effectiveDate;
      return this;
    }

    public Builder withEffectiveDateOrder(Integer effectiveDateOrder) {
      this.effectiveDateOrder = effectiveDateOrder;
      return this;
    }

    public Builder withCorrectionReference(String correctionReference) {
      this.correctionReference = correctionReference;
      return this;
    }

    public Builder withChanges(List<LicencePositionChangeType> changes) {
      this.changes = changes;
      return this;
    }

    public CreateLicencePositionPayload build() {
      return new CreateLicencePositionPayload(
          licencePositionId,
          licenceTransactionId,
          effectiveDate,
          effectiveDateOrder,
          correctionReference,
          changes
      );
    }
  }
}