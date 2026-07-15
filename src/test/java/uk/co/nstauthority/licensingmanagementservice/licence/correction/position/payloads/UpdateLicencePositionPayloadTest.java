package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

class UpdateLicencePositionPayloadTest {

  @Test
  void builder() {
    LicencePositionChangeType change = LicencePositionChangeType.addLicencePositionChange()
        .withChangeId("change-1")
        .withChangeOrder(1)
        .withOperations(List.of())
        .build();
    var changes = List.of(change);

    var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 5))
        .withEffectiveDateOrder(2)
        .withChanges(changes)
        .build();

    var expected = new UpdateLicencePositionPayload(
        LocalDate.of(2026, Month.JUNE, 5),
        2,
        changes
    );

    assertThat(payload).usingRecursiveComparison().isEqualTo(expected);
    assertThat(payload.type()).isEqualTo(LicencePositionPayload.UPDATE_POSITION);
  }
}