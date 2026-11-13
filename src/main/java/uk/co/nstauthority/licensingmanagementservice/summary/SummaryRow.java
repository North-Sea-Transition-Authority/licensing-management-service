package uk.co.nstauthority.licensingmanagementservice.summary;

import jakarta.annotation.Nullable;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;

/**
 * One side-by-side comparison, e.g. a block or subarea before and after a change. Each column's heading, map and
 * cards are lined up with the other columns', so they stay aligned when one column's map or heading is taller.
 */
public record SummaryRow(
    @Nullable String heading,
    @Nullable Tag tag,
    List<SummaryColumn> columns
) {

  public SummaryRow(List<SummaryColumn> columns) {
    this(null, null, columns);
  }
}
