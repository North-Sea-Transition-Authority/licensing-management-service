package uk.co.nstauthority.licensingmanagementservice.summary;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * One side of a {@link SummaryRow}. The heading, map (or inset text in its place) and cards each line up with the
 * same part of the other columns in the row; multiple cards stack within the cards slot.
 */
public record SummaryColumn(
    @Nullable String heading,
    @Nullable SummaryMapView map,
    List<SummaryCard> summaryCards,
    @Nullable String insetText
) {

  public SummaryColumn(@Nullable String heading, @Nullable SummaryMapView map, List<SummaryCard> summaryCards) {
    this(heading, map, summaryCards, null);
  }
}
