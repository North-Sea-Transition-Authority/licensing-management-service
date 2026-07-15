package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import java.time.LocalDate;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

public record UpdateLicencePositionPayload(
    LocalDate effectiveDate,
    Integer effectiveDateOrder,
    List<LicencePositionChangeType> changes
) implements LicencePositionPayload {

  @Override
  public String type() {
    return UPDATE_POSITION;
  }

  public static class Builder {

    private LocalDate effectiveDate;
    private Integer effectiveDateOrder;
    private List<LicencePositionChangeType> changes = List.of();

    public UpdateLicencePositionPayload.Builder withEffectiveDate(LocalDate effectiveDate) {
      this.effectiveDate = effectiveDate;
      return this;
    }

    public UpdateLicencePositionPayload.Builder withEffectiveDateOrder(Integer effectiveDateOrder) {
      this.effectiveDateOrder = effectiveDateOrder;
      return this;
    }

    public UpdateLicencePositionPayload.Builder withChanges(List<LicencePositionChangeType> changes) {
      this.changes = changes;
      return this;
    }

    public UpdateLicencePositionPayload build() {
      return new UpdateLicencePositionPayload(
          effectiveDate,
          effectiveDateOrder,
          changes
      );
    }
  }
}