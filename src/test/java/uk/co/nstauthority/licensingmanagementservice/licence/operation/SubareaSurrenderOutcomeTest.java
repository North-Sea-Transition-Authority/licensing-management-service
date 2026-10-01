package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.fivium.grpc.gis.IntersectionStatus;

class SubareaSurrenderOutcomeTest {

  private static final SubareaDetails SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails CROPPED_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");

  @Test
  void kept() {
    var result = SubareaSurrenderOutcome.kept(SUBAREA);

    assertThat(result).isEqualTo(new SubareaSurrenderOutcome(SUBAREA, IntersectionStatus.FULLY_INSIDE, null));
  }

  @Test
  void relinquished() {
    var result = SubareaSurrenderOutcome.relinquished(SUBAREA);

    assertThat(result).isEqualTo(new SubareaSurrenderOutcome(SUBAREA, IntersectionStatus.FULLY_OUTSIDE, null));
  }

  @Test
  void cropped() {
    var result = SubareaSurrenderOutcome.cropped(SUBAREA, CROPPED_SUBAREA);

    assertThat(result).isEqualTo(new SubareaSurrenderOutcome(SUBAREA, IntersectionStatus.CROPPED, CROPPED_SUBAREA));
  }

  @ParameterizedTest
  @EnumSource(
      value = IntersectionStatus.class,
      names = {"FULLY_INSIDE", "FULLY_OUTSIDE", "CROPPED"},
      mode = EnumSource.Mode.EXCLUDE)
  void constructor_whenStatusNotAnIntersection_thenThrows(IntersectionStatus status) {
    assertThatThrownBy(() -> new SubareaSurrenderOutcome(SUBAREA, status, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("status must be FULLY_INSIDE, FULLY_OUTSIDE or CROPPED but was %s".formatted(status));
  }

  @Test
  void constructor_whenCroppedWithoutCroppedSubarea_thenThrows() {
    assertThatThrownBy(() -> new SubareaSurrenderOutcome(SUBAREA, IntersectionStatus.CROPPED, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("croppedSubarea must be present only when status is CROPPED");
  }

  @ParameterizedTest
  @EnumSource(value = IntersectionStatus.class, names = {"FULLY_INSIDE", "FULLY_OUTSIDE"})
  void constructor_whenNotCroppedWithCroppedSubarea_thenThrows(IntersectionStatus status) {
    assertThatThrownBy(() -> new SubareaSurrenderOutcome(SUBAREA, status, CROPPED_SUBAREA))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("croppedSubarea must be present only when status is CROPPED");
  }

  @ParameterizedTest
  @MethodSource("outcomesAndOutputSubareas")
  void outputSubarea(SubareaSurrenderOutcome outcome, SubareaDetails expectedOutputSubarea) {
    var result = outcome.outputSubarea();

    assertThat(result).isEqualTo(expectedOutputSubarea);
  }

  private static Stream<Arguments> outcomesAndOutputSubareas() {
    return Stream.of(
        Arguments.of(SubareaSurrenderOutcome.kept(SUBAREA), SUBAREA),
        Arguments.of(SubareaSurrenderOutcome.cropped(SUBAREA, CROPPED_SUBAREA), CROPPED_SUBAREA),
        Arguments.of(SubareaSurrenderOutcome.relinquished(SUBAREA), null)
    );
  }

  @Test
  void constructor_whenSubareaNull_thenThrows() {
    assertThatThrownBy(() -> new SubareaSurrenderOutcome(null, IntersectionStatus.FULLY_OUTSIDE, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("subarea must not be null");
  }
}
