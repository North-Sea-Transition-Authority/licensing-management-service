package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableView;

@Service
public class CrossLicenceEventTrackerService {

  public SortableTableView getEventTrackerTable() {
    return SortableTableView.sortableTableBuilder()
        .newWithHeadings(
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
  }
}
