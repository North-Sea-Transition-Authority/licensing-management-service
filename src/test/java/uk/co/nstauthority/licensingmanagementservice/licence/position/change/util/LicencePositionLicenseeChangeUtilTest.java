package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionAddOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenseeChangeContext;

class LicencePositionLicenseeChangeUtilTest {
  private static final List<Integer> LICENSEES_TO_ADD = List.of(2, 3, 4);
  private static final List<Integer> LICENSEES_TO_REMOVE= List.of(1);

  @Test
  void upsertAddAdminChange_whenNoAdminChangeExists_appendsAddChange() {
    var result = LicencePositionLicenseeChangeUtil.upsertAddLicenseeChange(List.of(), LICENSEES_TO_ADD, LICENSEES_TO_REMOVE);

    assertThat(result).hasSize(1);
    assertThat(licenseesToAddOf(result.getFirst())).isEqualTo(LICENSEES_TO_ADD);
    assertThat(licenseesToRemoveOf(result.getFirst())).isEqualTo(LICENSEES_TO_REMOVE);
  }

  @Test
  void upsertAddAdminChange_whenAdminChangeExists_replacesOperatorInPlace() {
    var existing = licenseeAddChange();

    var result = LicencePositionLicenseeChangeUtil.upsertAddLicenseeChange(List.of(existing), List.of(5, 6), List.of(10));

    assertThat(result).hasSize(1);
    assertThat(licenseesToAddOf(result.getFirst())).isEqualTo(List.of(5, 6));
    assertThat(licenseesToRemoveOf(result.getFirst())).isEqualTo(List.of(10));
  }

  @Test
  void populateLicenseeForm() {
    var licenseeChangeContext = new LicenseeChangeContext(LICENSEES_TO_ADD, LICENSEES_TO_REMOVE, List.of(10), List.of("Ten"));

    var form = LicencePositionLicenseeChangeUtil.populateLicenseeForm(licenseeChangeContext);

    assertThat(form.getJoiningOrganisationIds()).isEqualTo(List.of("2", "3", "4"));
    assertThat(form.getWithdrawingOrganisationIds()).isEqualTo(List.of("1"));
  }

  @Test
  void licenseeChangeExists() {
    assertThat(LicencePositionLicenseeChangeUtil.licenseeChangeExists(
        List.of(licenseeAddChange()))).isTrue();
  }

  private LicencePositionChangeType licenseeAddChange() {
    var licenseeOperation = LicenceOperation.newLicenseeOperation()
        .withLicenseesToAdd(LICENSEES_TO_ADD)
        .withLicenseesToRemove(LICENSEES_TO_REMOVE)
        .build();
    var operation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(licenseeOperation.id())
        .withOperation(licenseeOperation)
        .build();
    return LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(1)
        .withOperations(List.of(operation))
        .build();
  }

  private List<Integer> licenseesToRemoveOf(LicencePositionChangeType change) {
    var addOperation = (LicencePositionAddOperation) (change).operations().getFirst();
    return ((LicenseeOperation) addOperation.operation()).licenseesToRemove();
  }

  private List<Integer> licenseesToAddOf(LicencePositionChangeType change) {
    var addOperation = (LicencePositionAddOperation) ( change).operations().getFirst();
    return ((LicenseeOperation) addOperation.operation()).licenseesToAdd();
  }
}
