package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

public record ScheduleWorkProgrammeApplicationContext(
    String reference,
    String type,
    List<SummaryDataView> summaryDataView
) {
}
