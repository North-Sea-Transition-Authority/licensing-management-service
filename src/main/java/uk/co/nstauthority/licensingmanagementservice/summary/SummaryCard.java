package uk.co.nstauthority.licensingmanagementservice.summary;

import java.util.List;
import java.util.stream.Stream;

public record SummaryCard(
    String displayName,
    SummaryCardType summaryCardType,
    Object summaryData,
    List<SummaryCardAction> actions
) {

  public static SummaryCard simpleSummaryCardWithHeading(String displayName,
                                                         SummaryDataView summaryData) {
    return new SummaryCard(
        displayName,
        SummaryCardType.SIMPLE_SUMMARY,
        summaryData,
        List.of()
    );
  }

  public static SummaryCard simpleSummaryCard(SummaryDataView summaryData) {
    return simpleSummaryCardWithHeading(null, summaryData);
  }

  public static SummaryCard emptySummaryCard() {
    return new SummaryCard(
        null,
        SummaryCardType.EMPTY_SUMMARY,
        null,
        List.of()
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
        summaryData,
        List.of()
    );
  }

  public static SummaryCard tableSummaryCard(SummaryTableView summaryData) {
    return tableSummaryCardWithHeading(null, summaryData);
  }

  public static SummaryCard filesSummaryCardWithHeading(String heading, List<SummaryFileView> fileViews) {
    return new SummaryCard(
        heading,
        SummaryCardType.FILES_SUMMARY,
        fileViews,
        List.of()
    );
  }

  public static SummaryCard filesAndDetailsSummaryCard(
      String heading,
      SummaryFileAndDetailsView summaryFileAndDetailsView
  ) {
    return new SummaryCard(
        heading,
        SummaryCardType.FILES_AND_DETAILS_SUMMARY,
        summaryFileAndDetailsView,
        List.of()
    );
  }

  public SummaryCard withAction(SummaryCardAction action) {
    return new SummaryCard(
        displayName,
        summaryCardType,
        summaryData,
        Stream.concat(actions.stream(), Stream.of(action)).toList()
    );
  }
}
