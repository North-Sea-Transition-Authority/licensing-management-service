package uk.co.nstauthority.licensingmanagementservice.summary;

import java.util.List;

public record SummaryCard(
    String displayName,
    SummaryCardType summaryCardType,
    Object summaryData
) {

  public static SummaryCard simpleSummaryCardWithHeading(String displayName,
                                                         SummaryDataView summaryData) {
    return new SummaryCard(
        displayName,
        SummaryCardType.SIMPLE_SUMMARY,
        summaryData
    );
  }

  public static SummaryCard simpleSummaryCard(SummaryDataView summaryData) {
    return simpleSummaryCardWithHeading(null, summaryData);
  }

  public static SummaryCard emptySummaryCard() {
    return new SummaryCard(
        null,
        SummaryCardType.EMPTY_SUMMARY,
        null
    );
  }

  public static List<SummaryCard> emptySummaryCardList() {
    return List.of(emptySummaryCard());
  }

  public static SummaryCard tableSummaryCardWithHeading(
      String displayName,
      SummaryTableView summaryData
  ) {
    return new SummaryCard(
        displayName,
        SummaryCardType.TABLE_SUMMARY,
        summaryData
    );
  }

  public static SummaryCard tableSummaryCard(SummaryTableView summaryData) {
    return tableSummaryCardWithHeading(null, summaryData);
  }

  public static SummaryCard filesSummaryCardWithHeading(String heading, List<SummaryFileView> fileViews) {
    return new SummaryCard(
        heading,
        SummaryCardType.FILES_SUMMARY,
        fileViews
    );
  }

  public static SummaryCard filesAndDetailsSummaryCard(
      String heading,
      SummaryFileAndDetailsView summaryFileAndDetailsView
  ) {
    return new SummaryCard(
        heading,
        SummaryCardType.FILES_AND_DETAILS_SUMMARY,
        summaryFileAndDetailsView
    );
  }
}
