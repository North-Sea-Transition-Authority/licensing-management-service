package uk.co.nstauthority.template.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryTableViewTest {

  private static final String HEADING1 = "Heading 1";
  private static final String HEADING2 = "Heading 2";
  private static final String HEADING3 = "Heading 3";
  private static final String HEADING4 = "Heading 4";

  private static final String STRING_ROW_VALUE = "1";

  private static final Integer INTEGER_ROW_VALUE = 1;

  private static final BigDecimal BIG_DECIMAL_ROW_VALUE = BigDecimal.ONE;

  @Test
  void newWithHeading() {
    assertThat(SummaryTableView.newWithHeading(HEADING1, HEADING2, HEADING3, HEADING4))
        .isEqualTo(
            new SummaryTableView(
                List.of(new SummaryTableRow(List.of(HEADING1, HEADING2, HEADING3, HEADING4)))
            )
        );
  }

  @Test
  void addRow() {
    var values = new ArrayList<String>();
    values.add(null);
    values.add(STRING_ROW_VALUE);
    values.add(STRING_ROW_VALUE);
    values.add(STRING_ROW_VALUE);
    var expectedSummaryTable =
        new SummaryTableView(
            List.of(
                new SummaryTableRow(List.of(HEADING1, HEADING2, HEADING3, HEADING4)),
                new SummaryTableRow(values)
            )
        );

    var summaryTable = SummaryTableView.newWithHeading(HEADING1, HEADING2, HEADING3, HEADING4);

    assertThat(summaryTable.addRow(null, STRING_ROW_VALUE, INTEGER_ROW_VALUE, BIG_DECIMAL_ROW_VALUE))
        .isEqualTo(expectedSummaryTable);
  }

  @Test
  void addRow_valueCountError() {
    var summaryTable = SummaryTableView.newWithHeading(HEADING1, HEADING2, HEADING3, HEADING4);
    assertThatThrownBy(() -> summaryTable.addRow(STRING_ROW_VALUE, INTEGER_ROW_VALUE, BIG_DECIMAL_ROW_VALUE))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Error adding to summary table, all table rows must have the same number of values, expected value count 3");
  }

  @Test
  void addRow_formatError() {
    var summaryTable = SummaryTableView.newWithHeading(HEADING1);
    assertThatThrownBy(() -> summaryTable.addRow(1L))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Unexpected value class type: %s".formatted(Long.class.getName()));
  }
}
