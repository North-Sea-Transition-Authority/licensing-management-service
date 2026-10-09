package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LicenseeOperationTest {

  @Test
  void type_isLicensees() {
    assertThat((new LicenseeOperation(LicenseeOperation.LICENSEE_OPERATION_ID, List.of(), List.of())).type())
        .isEqualTo(LicenceOperation.LICENSEE);
  }

  @Test
  void displayName() {
    assertThat(new LicenseeOperation(LicenseeOperation.LICENSEE_OPERATION_ID, List.of(), List.of()).displayName())
        .isEqualTo("Licensee change");
  }

  @Test
  void constructor() {
    var licenseeOperation = LicenceOperation.newLicenseeOperation()
        .withLicenseesToAdd(List.of(12, 34))
        .withLicenseesToRemove(List.of(56))
        .build();

    assertThat(licenseeOperation.licenseesToAdd()).isEqualTo(List.of(12, 34));
    assertThat(licenseeOperation.licenseesToRemove()).isEqualTo(List.of(56));
  }

  @Test
  void organisationUnitIds() {
    var operation = LicenceOperation.newLicenseeOperation()
        .withLicenseesToAdd(List.of(12, 34))
        .withLicenseesToRemove(List.of(34, 56))
        .build();

    var result = operation.organisationUnitIds();

    assertThat(result).containsExactlyInAnyOrder(12, 34, 56);
  }
}
