package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

public record LicenseeOperation(
    UUID id,
    List<Integer> licenseesToAdd,
    List<Integer> licenseesToRemove
) implements VisibleLicenceOperation {

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
  public Set<Integer> organisationUnitIds() {
    return Stream.concat(licenseesToAdd.stream(), licenseesToRemove.stream()).collect(Collectors.toSet());
  }

  @Override
  public PositionValidationError validate(PositionValidationContext positionValidationContext) {
    return null;
  }

  @Override
  public LicencePositionState applyState(LicencePositionState licencePositionState) {
    return licencePositionState.withLicenseeIds(licenseesToAdd, licenseesToRemove);
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
