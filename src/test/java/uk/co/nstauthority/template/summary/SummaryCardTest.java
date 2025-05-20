package uk.co.nstauthority.template.summary;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryCardTest {

  private static final String HEADING1 = "Heading 1";
  private static final String HEADING2 = "Heading 2";
  private static final String HEADING3 = "Heading 3";
  private static final String HEADING4 = "Heading 4";
  private static final String STRING_ROW_VALUE = "1";
  private static final Integer INTEGER_ROW_VALUE = 1;
  private static final BigDecimal BIG_DECIMAL_ROW_VALUE = BigDecimal.ONE;

  private final SummaryTableView summaryTableView = SummaryTableView.newWithHeading(HEADING1, HEADING2, HEADING3, HEADING4)
      .addRow(null, STRING_ROW_VALUE, INTEGER_ROW_VALUE, BIG_DECIMAL_ROW_VALUE);

  private static final List<SummaryKeyValue> SUMMARY_KEY_VALUES = List.of(
      new SummaryKeyValue("key1", SummaryValueType.STRING_VALUE, List.of("value1")),
      new SummaryKeyValue("key2", SummaryValueType.STRING_VALUE, List.of("value2"))
  );

  private static final SummaryDataView SUMMARY_DATA_VIEW = new SummaryDataView(SUMMARY_KEY_VALUES);

  private static final SummaryFileView SUMMARY_FILE_VIEW_1 = new SummaryFileView("Key 1", List.of(
      new UploadedFileView(
          "file id",
          "file name",
          "file size",
          "file desc",
          Instant.parse("2025-04-10T10:25:00Z"),
          "https://www.fivium.co.uk"
      )));
  private static final SummaryFileView SUMMARY_FILE_VIEW_2 = new SummaryFileView("Key 2", Collections.emptyList());
  private static final List<SummaryFileView> SUMMARY_FILE_VIEWS = List.of(SUMMARY_FILE_VIEW_1, SUMMARY_FILE_VIEW_2);


  @Test
  void simpleSummaryCardWithHeading() {
    assertThat(SummaryCard.simpleSummaryCardWithHeading("display name", SUMMARY_DATA_VIEW))
        .isEqualTo(
            new SummaryCard(
                "display name",
                SummaryCardType.SIMPLE_SUMMARY,
                SUMMARY_DATA_VIEW
            )
        );
  }

  @Test
  void simpleSummaryCard() {
    assertThat(SummaryCard.simpleSummaryCard(SUMMARY_DATA_VIEW))
        .isEqualTo(
            new SummaryCard(
                null,
                SummaryCardType.SIMPLE_SUMMARY,
                SUMMARY_DATA_VIEW
            )
        );
  }

  @Test
  void emptySummaryCard() {
    assertThat(SummaryCard.emptySummaryCard())
        .isEqualTo(
            new SummaryCard(
                null,
                SummaryCardType.EMPTY_SUMMARY,
                null
            )
        );
  }

  @Test
  void emptySummaryCardList() {
    assertThat(SummaryCard.emptySummaryCardList())
        .isEqualTo(
            List.of(
                new SummaryCard(
                    null,
                    SummaryCardType.EMPTY_SUMMARY,
                    null
                )
            )
        );
  }

  @Test
  void tableSummaryCardWithHeading() {
    assertThat(SummaryCard.tableSummaryCardWithHeading("display name", summaryTableView))
        .isEqualTo(
            new SummaryCard(
                "display name",
                SummaryCardType.TABLE_SUMMARY,
                summaryTableView
            )
        );
  }

  @Test
  void tableSummaryCard() {
    assertThat(SummaryCard.tableSummaryCard(summaryTableView))
        .isEqualTo(
            new SummaryCard(
                null,
                SummaryCardType.TABLE_SUMMARY,
                summaryTableView
            )
        );
  }

  @Test
  void filesSummaryCardWithHeading() {
    assertThat(SummaryCard.filesSummaryCardWithHeading("display name", SUMMARY_FILE_VIEWS))
        .isEqualTo(
            new SummaryCard(
                "display name",
                SummaryCardType.FILES_SUMMARY,
                SUMMARY_FILE_VIEWS
            )
        );
  }
}
