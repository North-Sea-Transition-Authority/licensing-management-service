package uk.co.nstauthority.template.summary;

import java.util.List;

public record SummarySection(
    int displayOrder,
    List<SummaryItem> summaryItems
) {
}
