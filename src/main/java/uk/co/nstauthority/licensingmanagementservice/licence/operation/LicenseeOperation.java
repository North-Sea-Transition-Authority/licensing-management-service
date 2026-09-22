package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

public record LicenseeOperation(
    UUID id,
    List<Integer> licenseesToAdd,
    List<Integer> licenseesToRemove
) implements LicenceOperation {

  public static final UUID LICENSEE_OPERATION_ID = new UUID(0L, 0L);

  @Override
  public String type() {
    return LICENSEE;
  }

  @Override
  public String displayName() {
    return "Licensee change";
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    return null;
  }

  public static class Builder {

    private List<Integer> licenseesToAdd = List.of();
    private List<Integer> licenseesToRemove = List.of();

    public LicenseeOperation.Builder withLicenseesToAdd(List<Integer> licenseesToAdd) {
      this.licenseesToAdd = licenseesToAdd;
      return this;
    }

    public LicenseeOperation.Builder withLicenseesToRemove(List<Integer> licenseesToRemove) {
      this.licenseesToRemove = licenseesToRemove;
      return this;
    }

    public LicenseeOperation build() {
      return new LicenseeOperation(LICENSEE_OPERATION_ID, licenseesToAdd, licenseesToRemove);
    }
  }
}
