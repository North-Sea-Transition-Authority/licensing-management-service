package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PearsOperationTest {

  private static final int REPLACED_SI_ID = 194523;
  private static final int SECOND_REPLACED_SI_ID = 194524;

  static Stream<Arguments> successorAreas() {
    return Stream.of(
        arguments("one successor smaller than the block it replaced", "242.84", List.of("226.67"), true),
        arguments("successors leaving the block short", "100", List.of("60", "20"), true),
        arguments("successors covering the block", "100", List.of("60", "40"), false),
        arguments("successors covering more than the block", "100", List.of("60", "41"), false),
        arguments("successors short by less than the tolerance", "100", List.of("59.95", "40"), false),
        arguments("successors short by more than the tolerance", "100", List.of("59.8", "40"), true)
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("successorAreas")
  void surrenderedInputSiIds_whenTheAreasAreKnown_thenOnlyAShortfallBeyondTheToleranceIsASurrender(
      String description,
      String replacedArea,
      List<String> successorAreas,
      boolean surrenders
  ) {
    var change = blockChange(successorAreas.stream()
        .map(successorArea -> entry(REPLACED_SI_ID, replacedArea, successorArea))
        .toList());

    var result = change.surrenderedInputSiIds();

    assertThat(result).isEqualTo(surrenders ? Set.of(REPLACED_SI_ID) : Set.of());
  }

  @Test
  void surrenderedInputSiIds_whenAChangeActsOnSeveralBlocks_thenEachIsWeighedOnItsOwnSuccessors() {
    var change = blockChange(List.of(
        entry(REPLACED_SI_ID, "100", "60"),
        entry(REPLACED_SI_ID, "100", "40"),
        entry(SECOND_REPLACED_SI_ID, "100", "70")
    ));

    var result = change.surrenderedInputSiIds();

    assertThat(result).isEqualTo(Set.of(SECOND_REPLACED_SI_ID));
  }

  @Test
  void surrenderedInputSiIds_whenASuccessorAreaIsMissing_thenTheBlockGaveUpNothing() {
    var change = blockChange(List.of(
        entry(REPLACED_SI_ID, "100", "60"),
        entry(REPLACED_SI_ID, "100", null)
    ));

    var result = change.surrenderedInputSiIds();

    assertThat(result).isEmpty();
  }

  @Test
  void surrenderedInputSiIds_whenTheReplacedAreaIsMissing_thenTheBlockGaveUpNothing() {
    var change = blockChange(List.of(entry(REPLACED_SI_ID, null, "60")));

    var result = change.surrenderedInputSiIds();

    assertThat(result).isEmpty();
  }

  static Stream<Arguments> carriedWithItsBlock() {
    return Stream.of(
        arguments(PearsOperation.EntryType.TRANSFER, false, false, true),
        arguments(PearsOperation.EntryType.SET, true, true, false),
        arguments(PearsOperation.EntryType.REMOVE, true, true, false),
        arguments(null, true, true, true),
        arguments(null, true, false, false),
        arguments(null, false, true, false)
    );
  }

  @ParameterizedTest(name = "{0}, input present {1}, output present {2}")
  @MethodSource("carriedWithItsBlock")
  void isCarriedWithItsBlock(
      PearsOperation.EntryType type,
      boolean hasInput,
      boolean hasOutput,
      boolean expected
  ) {
    var entry = subareaEntry(type, hasInput, hasOutput);

    var result = entry.isCarriedWithItsBlock();

    assertThat(result).isEqualTo(expected);
  }

  static Stream<Arguments> outputSide() {
    return Stream.of(
        arguments(PearsOperation.EntryType.SET, false, false, true),
        arguments(PearsOperation.EntryType.TRANSFER, false, false, true),
        arguments(PearsOperation.EntryType.REMOVE, true, true, false),
        arguments(null, false, true, true),
        arguments(null, true, false, false)
    );
  }

  @ParameterizedTest(name = "{0}, input present {1}, output present {2}")
  @MethodSource("outputSide")
  void hasOutputSide(
      PearsOperation.EntryType type,
      boolean hasInput,
      boolean hasOutput,
      boolean expected
  ) {
    var entry = subareaEntry(type, hasInput, hasOutput);

    var result = entry.hasOutputSide();

    assertThat(result).isEqualTo(expected);
  }

  static Stream<Arguments> inputSide() {
    return Stream.of(
        arguments(PearsOperation.EntryType.REMOVE, false, false, true),
        arguments(PearsOperation.EntryType.TRANSFER, false, false, true),
        arguments(PearsOperation.EntryType.SET, true, true, false),
        arguments(null, true, false, true),
        arguments(null, false, true, false)
    );
  }

  @ParameterizedTest(name = "{0}, input present {1}, output present {2}")
  @MethodSource("inputSide")
  void hasInputSide(
      PearsOperation.EntryType type,
      boolean hasInput,
      boolean hasOutput,
      boolean expected
  ) {
    var entry = subareaEntry(type, hasInput, hasOutput);

    var result = entry.hasInputSide();

    assertThat(result).isEqualTo(expected);
  }

  private static PearsOperation.SubareaEntry subareaEntry(
      PearsOperation.EntryType type,
      boolean hasInput,
      boolean hasOutput
  ) {
    return new PearsOperation.SubareaEntry(
        type,
        null,
        "SA",
        hasInput ? new PearsOperation.Subarea(739101, "Subarea", REPLACED_SI_ID, null) : null,
        hasOutput ? new PearsOperation.Subarea(739102, "Subarea", SECOND_REPLACED_SI_ID, null) : null
    );
  }

  private static PearsOperation.BlockChange blockChange(List<PearsOperation.BlockEntry> entries) {
    return new PearsOperation.BlockChange(
        new PearsOperation.Header(
            15188,
            "XPT/1",
            LocalDate.of(1964, Month.SEPTEMBER, 18),
            6,
            1000,
            1,
            PearsOperation.OperationStatus.LIVE,
            null,
            null
        ),
        entries,
        List.of()
    );
  }

  private static PearsOperation.BlockEntry entry(
      int replacedSiId,
      String replacedArea,
      String successorArea
  ) {
    return new PearsOperation.BlockEntry(
        PearsOperation.EntryType.TRANSFER,
        null,
        block(replacedSiId, replacedArea),
        block(replacedSiId + 500_000, successorArea)
    );
  }

  private static PearsOperation.Block block(
      int siId,
      String areaKm2
  ) {
    return new PearsOperation.Block(
        siId,
        null,
        null,
        null,
        null,
        areaKm2 == null ? null : new BigDecimal(areaKm2)
    );
  }
}
