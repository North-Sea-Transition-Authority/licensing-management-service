package uk.co.nstauthority.licensingmanagementservice.summary;

import java.util.List;

public record SummarySection(
    int displayOrder,
    List<SummaryItem> summaryItems
) {
}
