package uk.co.nstauthority.licensingmanagementservice.summary;

import jakarta.annotation.Nullable;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;

public record SummaryItem(
    String displayName,
    List<SummaryCard> summaryCards,
    List<SummaryRow> summaryRows,
    @Nullable Tag tag
) {

  public SummaryItem(String displayName, List<SummaryCard> summaryCards) {
    this(displayName, summaryCards, List.of(), null);
  }

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

  public static SummaryItem withRows(String displayName,
                                     List<SummaryRow> summaryRows) {
    return new SummaryItem(displayName, List.of(), summaryRows, null);
  }

  public SummaryItem withTag(Tag tag) {
    return new SummaryItem(displayName, summaryCards, summaryRows, tag);
  }
}
