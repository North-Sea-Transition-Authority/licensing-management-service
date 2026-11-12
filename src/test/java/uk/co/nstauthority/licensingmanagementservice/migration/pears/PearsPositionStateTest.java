package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PearsPositionStateTest {

  private static final LocalDate FIRST_DATE = LocalDate.of(2019, Month.APRIL, 1);
  private static final LocalDate SECOND_DATE = LocalDate.of(2020, Month.JUNE, 30);

  @Test
  void byPosition_whenRowsSpanPositions_thenGroupsWhatEachPositionHolds() {
    var rows = List.of(
        new PearsPositionState.Row(FIRST_DATE, 6, "LICENCE_ADMINISTRATOR", 20, null, null, null),
        new PearsPositionState.Row(FIRST_DATE, 6, "LICENSEE", 20, null, null, null),
        new PearsPositionState.Row(FIRST_DATE, 6, "LICENSEE", 21, null, null, null),
        new PearsPositionState.Row(FIRST_DATE, 6, "BLOCK", null, 194508, null, null),
        new PearsPositionState.Row(FIRST_DATE, 6, "BLOCK", null, null, null, null),
        new PearsPositionState.Row(FIRST_DATE, 6, "SUBAREA", null, 194508, 194542, "ALL"),
        new PearsPositionState.Row(SECOND_DATE, 3, "LICENSEE", 21, null, null, null)
    );

    var states = PearsPositionState.byPosition(rows);

    var withShapelessBlock = new HashSet<Integer>();
    withShapelessBlock.add(194508);
    withShapelessBlock.add(null);
    assertThat(states).isEqualTo(Map.of(
        new PearsLicencePositions.PositionKey(FIRST_DATE, 6), new PearsPositionState(
            Set.of(20),
            Set.of(20, 21),
            withShapelessBlock,
            Set.of(new PearsPositionState.Subarea(194508, 194542, "ALL"))
        ),
        new PearsLicencePositions.PositionKey(SECOND_DATE, 3), new PearsPositionState(
            Set.of(),
            Set.of(21),
            Set.of(),
            Set.of()
        )
    ));
  }

  @Test
  void byPosition_whenARowHoldsSomethingUnknown_thenThrows() {
    var rows = List.of(new PearsPositionState.Row(FIRST_DATE, 6, "SUBAREA_OPERATOR", 20, null, null, null));

    assertThatThrownBy(() -> PearsPositionState.byPosition(rows))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unexpected held kind SUBAREA_OPERATOR");
  }
}
