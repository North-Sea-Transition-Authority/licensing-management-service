package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;

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

  @Test
  void getChangeView() {
    var positionChange = PositionChangeTestUtil.newBuilder().withChangeType("licensee").build();
    var previousState = LicencePositionState.EMPTY;
    var context = new LicencePositionChangeViewContext(Map.of(1, "Zeta", 2, "Beta"), Map.of());
    var urls = ChangeViewUrls.none();
    var licenseeOperation = LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(1, 2)).build();
    var expectedChangeView = new LicenseeChangeView(
        List.of(),
        List.of("Beta", "Zeta"),
        positionChange.changeType(),
        urls
    );

    assertThat(licenseeOperation.getChangeView(
        positionChange,
        previousState,
        null,
        context,
        urls
    )).isEqualTo(expectedChangeView);
  }
}
