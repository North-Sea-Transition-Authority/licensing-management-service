package uk.co.nstauthority.licensingmanagementservice.summary;

import java.util.List;

public record SummarySection(
    int displayOrder,
    List<SummaryItem> summaryItems,
    SummarySectionDisplay display
) {

  public SummarySection(int displayOrder, List<SummaryItem> summaryItems) {
    this(displayOrder, summaryItems, SummarySectionDisplay.ACCORDION);
  }
}
