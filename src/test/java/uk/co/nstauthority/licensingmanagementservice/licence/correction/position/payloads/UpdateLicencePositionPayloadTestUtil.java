package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

public class UpdateLicencePositionPayloadTestUtil {

  private LocalDate effectiveDate = LocalDate.of(2026, Month.JUNE, 5);
  private Integer effectiveDateOrder = 1;
  private List<LicencePositionChangeType> changes = List.of();

  public static UpdateLicencePositionPayloadTestUtil newBuilder() {
    return new UpdateLicencePositionPayloadTestUtil();
  }

  public UpdateLicencePositionPayloadTestUtil withEffectiveDate(LocalDate effectiveDate) {
    this.effectiveDate = effectiveDate;
    return this;
  }

  public UpdateLicencePositionPayloadTestUtil withEffectiveDateOrder(Integer effectiveDateOrder) {
    this.effectiveDateOrder = effectiveDateOrder;
    return this;
  }

  public UpdateLicencePositionPayloadTestUtil withChanges(List<LicencePositionChangeType> changes) {
    this.changes = changes;
    return this;
  }

  public UpdateLicencePositionPayload build() {
    return LicencePositionPayload.newUpdateLicencePositionPayload()
        .withEffectiveDate(effectiveDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .withChanges(changes)
        .build();
  }
}