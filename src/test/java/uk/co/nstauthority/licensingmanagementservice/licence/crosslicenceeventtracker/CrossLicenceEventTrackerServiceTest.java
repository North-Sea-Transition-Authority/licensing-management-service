package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableRow;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableSortDirection;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableView;
import uk.co.nstauthority.licensingmanagementservice.fds.table.TableHeadingStyle;

class CrossLicenceEventTrackerServiceTest {

  private final CrossLicenceEventTrackerService crossLicenceEventTrackerService = new CrossLicenceEventTrackerService();

  @Test
  void getEventTrackerTable() {
    var result = crossLicenceEventTrackerService.getEventTrackerTable();

    var expectedHeadingRow = SortableTableRow.builder()
        .withValues(
            "Licence",
            "Term / phase transition",
            "Work programme activity",
            "Event end / due date",
            "Application status",
            "Licensee(s)",
            "Quad/block",
            "Steward"
        )
        .build();
    var expected = new SortableTableView(
        TableHeadingStyle.COLUMN,
        0,
        SortableTableSortDirection.ASCENDING.getFrontendSortValue(),
        null,
        List.of(expectedHeadingRow)
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
