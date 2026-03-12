package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

public record LicenceContinuationApplicationContext(
    String reference,
    String type,
    List<SummaryDataView> summaryDataView
) {
}