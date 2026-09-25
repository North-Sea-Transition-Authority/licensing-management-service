package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import java.util.List;

public record LicenceOperationHistory(
    String licenceType,
    int licenceNo,
    List<PearsOperation> operations
) {

  public String licenceReference() {
    return "%s%d".formatted(licenceType, licenceNo);
  }
}
