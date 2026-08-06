package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

class LicencePositionPayloadTest {

  private static final List<LicencePositionChangeType> NEW_CHANGES = List.of(
      LicencePositionChangeType.removeChange().withChangeId("change-id").build());

  @Test
  void withChanges_whenCreatePayload_replacesChangesAndPreservesOtherFields() {
    var original = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withLicencePositionId("position-id")
        .withLicenceTransactionId("transaction-id")
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 5))
        .withEffectiveDateOrder(3)
        .withCorrectionReference("CORRECTION-REF")
        .withChanges(List.of())
        .build();

    var result = LicencePositionPayload.withChanges(original, NEW_CHANGES);

    assertThat(result).isInstanceOf(CreateLicencePositionPayload.class);
    var payload = (CreateLicencePositionPayload) result;
    assertThat(payload.licencePositionId()).isEqualTo("position-id");
    assertThat(payload.licenceTransactionId()).isEqualTo("transaction-id");
    assertThat(payload.effectiveDate()).isEqualTo(LocalDate.of(2026, Month.JUNE, 5));
    assertThat(payload.effectiveDateOrder()).isEqualTo(3);
    assertThat(payload.correctionReference()).isEqualTo("CORRECTION-REF");
    assertThat(payload.changes()).isEqualTo(NEW_CHANGES);
  }

  @Test
  void withChanges_whenUpdatePayload_replacesChangesAndPreservesOtherFields() {
    var original = LicencePositionPayload.newUpdateLicencePositionPayload()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 5))
        .withEffectiveDateOrder(2)
        .withCorrectionReference("CORRECTION-REF")
        .withChanges(List.of())
        .build();

    var result = LicencePositionPayload.withChanges(original, NEW_CHANGES);

    assertThat(result).isInstanceOf(UpdateLicencePositionPayload.class);
    var payload = (UpdateLicencePositionPayload) result;
    assertThat(payload.effectiveDate()).isEqualTo(LocalDate.of(2026, Month.JUNE, 5));
    assertThat(payload.effectiveDateOrder()).isEqualTo(2);
    assertThat(payload.correctionReference()).isEqualTo("CORRECTION-REF");
    assertThat(payload.changes()).isEqualTo(NEW_CHANGES);
  }
}
