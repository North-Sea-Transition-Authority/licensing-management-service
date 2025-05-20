package uk.co.nstauthority.template.summary;

import java.util.List;

public record SummaryItem(
    String displayName,
    List<SummaryCard> summaryCards
) {

  public static SummaryItem withCard(String displayName,
                                     SummaryCard summaryCard) {
    return new SummaryItem(
        displayName,
        summaryCard != null
            ? List.of(summaryCard)
            : SummaryCard.emptySummaryCardList()
    );
  }

  public static SummaryItem withCards(String displayName,
                                      List<SummaryCard> summaryCards) {
    return new SummaryItem(
        displayName,
        summaryCards != null && !summaryCards.isEmpty()
            ? summaryCards
            : SummaryCard.emptySummaryCardList()
    );
  }
}
