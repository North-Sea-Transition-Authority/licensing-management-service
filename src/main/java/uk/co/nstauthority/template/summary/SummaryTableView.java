package uk.co.nstauthority.template.summary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record SummaryTableView(
    List<SummaryTableRow> tableRows
) {
  public static SummaryTableView newWithHeading(Object... rowValues) {
    var summaryTableView = new SummaryTableView(new ArrayList<>());
    return summaryTableView.addRow(rowValues);
  }

  public SummaryTableView addRow(Object... rowValues) {
    valueCountCheck(rowValues.length);
    var formattedRowValueStrings = Arrays.stream(rowValues).map(SummaryUtil::formatAsString).toList();
    tableRows.add(new SummaryTableRow(formattedRowValueStrings));
    return this;
  }

  private void valueCountCheck(int expectedValueCount) {
    var allRowsHaveEqualValueCount = tableRows.stream()
        .allMatch(summaryTableRow -> summaryTableRow.valueCount() == expectedValueCount);

    if (!allRowsHaveEqualValueCount) {
      throw new RuntimeException("Error adding to summary table, " +
          "all table rows must have the same number of values, expected value count %s"
              .formatted(expectedValueCount));
    }
  }
}
