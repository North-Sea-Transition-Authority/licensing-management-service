package uk.co.nstauthority.licensingmanagementservice.summary;

import java.util.List;

public record SummaryFileAndDetailsView(
    SummaryDataView summaryData,
    List<SummaryFileView> fileViews
) {
}