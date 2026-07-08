package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

class CreateLicencePositionPayloadTest {

  @Test
  void builder() {
    List<LicencePositionChangeType> changes = List.of();

    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId("position-id")
        .withLicenceTransactionId("transaction-id")
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 5))
        .withEffectiveDateOrder(2)
        .withCorrectionReference("CORRECTION-REF")
        .withChanges(changes)
        .build();

    assertThat(payload.licencePositionId()).isEqualTo("position-id");
    assertThat(payload.licenceTransactionId()).isEqualTo("transaction-id");
    assertThat(payload.effectiveDate()).isEqualTo(LocalDate.of(2026, Month.JUNE, 5));
    assertThat(payload.effectiveDateOrder()).isEqualTo(2);
    assertThat(payload.correctionReference()).isEqualTo("CORRECTION-REF");
    assertThat(payload.changes()).isEqualTo(changes);
    assertThat(payload.type()).isEqualTo(LicencePositionPayload.ADD_POSITION);
  }
}