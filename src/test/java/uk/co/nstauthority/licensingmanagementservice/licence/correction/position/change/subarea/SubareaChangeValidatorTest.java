package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;

@ExtendWith(MockitoExtension.class)
class SubareaChangeValidatorTest {

  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final SubareaDetails SUBAREA_A = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaDetails SUBAREA_B = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");

  @Mock
  private CommandJourneyService commandJourneyService;

  @Mock
  private OperatorCommandService operatorCommandService;

  @InjectMocks
  private SubareaChangeValidator subareaChangeValidator;

  @ParameterizedTest
  @CsvSource({
      "true, false",
      "false, true"
  })
  void hasErrors(boolean hasActiveCommand, boolean expectedHasErrors) {
    var commandJourney = new CommandJourney();
    when(commandJourneyService.getCommandJourneyOrThrow(COMMAND_JOURNEY_ID)).thenReturn(commandJourney);
    when(operatorCommandService.canUndo(commandJourney)).thenReturn(hasActiveCommand);

    var result = subareaChangeValidator.hasErrors(COMMAND_JOURNEY_ID);

    assertThat(result).isEqualTo(expectedHasErrors);
  }

  @Test
  void getErrors_whenNoTransfers_thenNoChangeError() {
    var result = subareaChangeValidator.getErrors(List.of(SUBAREA_A, SUBAREA_B), List.of());

    assertThat(result).containsExactly("Make a change to the subareas");
  }

  @Test
  void getErrors_whenMergedOutputListedInSeveralTransfers_thenNoErrors() {
    var merged = subarea("Subarea C", "C");
    var transfers = List.of(
        new SubareaTransfer(List.of(SUBAREA_A), List.of(merged)),
        new SubareaTransfer(List.of(SUBAREA_B), List.of(merged))
    );

    var result = subareaChangeValidator.getErrors(List.of(SUBAREA_A, SUBAREA_B), transfers);

    assertThat(result).isEmpty();
  }

  @Test
  void getErrors_whenOutputNamesAreUnique_thenNoErrors() {
    var transfers = List.of(new SubareaTransfer(
        List.of(SUBAREA_A),
        List.of(subarea("Subarea A1", "A1"), subarea("Subarea A2", "A2"))
    ));

    var result = subareaChangeValidator.getErrors(List.of(SUBAREA_A, SUBAREA_B), transfers);

    assertThat(result).isEmpty();
  }

  @Test
  void getErrors_whenOutputReusesReplacedSubareaNames_thenNoErrors() {
    var transfers = List.of(new SubareaTransfer(
        List.of(SUBAREA_A),
        List.of(subarea("Subarea A", "A"))
    ));

    var result = subareaChangeValidator.getErrors(List.of(SUBAREA_A, SUBAREA_B), transfers);

    assertThat(result).isEmpty();
  }

  @Test
  void getErrors_whenOutputDuplicatesRemainingMapDisplayName_thenMapDisplayNameError() {
    var transfers = List.of(new SubareaTransfer(
        List.of(SUBAREA_A),
        List.of(subarea("Subarea A1", "B"))
    ));

    var result = subareaChangeValidator.getErrors(List.of(SUBAREA_A, SUBAREA_B), transfers);

    assertThat(result).containsExactly("Map display names must be unique within a licence block");
  }

  @Test
  void getErrors_whenOutputDuplicatesRemainingSubareaName_thenSubareaNameError() {
    var transfers = List.of(new SubareaTransfer(
        List.of(SUBAREA_A),
        List.of(subarea("Subarea B", "A1"))
    ));

    var result = subareaChangeValidator.getErrors(List.of(SUBAREA_A, SUBAREA_B), transfers);

    assertThat(result).containsExactly("Subarea names must be unique within a licence block");
  }

  @Test
  void getErrors_whenOutputsDuplicateEachOther_thenBothNameErrors() {
    var transfers = List.of(new SubareaTransfer(
        List.of(SUBAREA_A),
        List.of(subarea("Subarea A1", "A1"), subarea("Subarea A1", "A1"))
    ));

    var result = subareaChangeValidator.getErrors(List.of(SUBAREA_A, SUBAREA_B), transfers);

    assertThat(result).containsExactly(
        "Subarea names must be unique within a licence block",
        "Map display names must be unique within a licence block"
    );
  }

  @Test
  void getErrors_whenDuplicatesExistedBeforeChangeAndRemain_thenBothNameErrors() {
    var duplicateOfB = subarea("Subarea B", "B");
    var transfers = List.of(new SubareaTransfer(
        List.of(SUBAREA_A),
        List.of(subarea("Subarea A1", "A1"))
    ));

    var result = subareaChangeValidator.getErrors(List.of(SUBAREA_A, SUBAREA_B, duplicateOfB), transfers);

    assertThat(result).containsExactly(
        "Subarea names must be unique within a licence block",
        "Map display names must be unique within a licence block"
    );
  }

  @Test
  void getErrors_whenChangeRenamesExistingDuplicate_thenNoErrors() {
    var duplicateOfB = subarea("Subarea B", "B");
    var transfers = List.of(new SubareaTransfer(
        List.of(duplicateOfB),
        List.of(subarea("Subarea B2", "B2"))
    ));

    var result = subareaChangeValidator.getErrors(List.of(SUBAREA_A, SUBAREA_B, duplicateOfB), transfers);

    assertThat(result).isEmpty();
  }

  private static SubareaDetails subarea(String name, String shortName) {
    return new SubareaDetails(UUID.randomUUID(), name, shortName);
  }
}
