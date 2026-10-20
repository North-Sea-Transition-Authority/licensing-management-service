package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

class UpdateLicencePositionPayloadTest {

  @Test
  void builder() {
    List<LicencePositionChangeType> changes = List.of(
        LicencePositionChangeType.addChange().withChangeId("change-id").build());

    var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 5))
        .withEffectiveDateOrder(2)
        .withCorrectionReference("CORRECTION-REF")
        .withChanges(changes)
        .build();

    assertThat(payload.effectiveDate()).isEqualTo(LocalDate.of(2026, Month.JUNE, 5));
    assertThat(payload.effectiveDateOrder()).isEqualTo(2);
    assertThat(payload.correctionReference()).isEqualTo("CORRECTION-REF");
    assertThat(payload.changes()).isEqualTo(changes);
    assertThat(payload.type()).isEqualTo(LicencePositionPayload.UPDATE_POSITION);
  }

  @Test
  void builder_whenNoChangesSet_defaultsToEmptyList() {
    assertThat(LicencePositionPayload.newUpdateLicencePositionPayload().build().changes()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("effectiveDatesAndWhetherTheyChangeThePositionDate")
  void changesPositionDate(LocalDate effectiveDate, Integer effectiveDateOrder, boolean expected) {
    var payload = LicencePositionPayload.newUpdateLicencePositionPayload()
        .withEffectiveDate(effectiveDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .build();

    assertThat(payload.changesPositionDate()).isEqualTo(expected);
  }

  private static Stream<Arguments> effectiveDatesAndWhetherTheyChangeThePositionDate() {
    var effectiveDate = LocalDate.of(2026, Month.JUNE, 5);

    return Stream.of(
        Arguments.of(null, null, false),
        Arguments.of(effectiveDate, null, true),
        Arguments.of(null, 2, true),
        Arguments.of(effectiveDate, 2, true)
    );
  }

}