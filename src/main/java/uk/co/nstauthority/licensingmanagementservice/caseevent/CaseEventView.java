package uk.co.nstauthority.licensingmanagementservice.caseevent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.UploadedFileView;

public record CaseEventView(
    CaseEventType eventType,
    Map<String, String> details,
    Map<String, List<UploadedFileView>> files,
    String eventBy,
    Instant eventInstant
) {

  static final String EVENT_BY_KEY = "Event by";
  static final String DATE_KEY = "Date";

  public CaseEventView(
      CaseEventType eventType,
      Map<String, String> details,
      String eventBy,
      Instant eventInstant
  ) {
    this(eventType, details, Map.of(), eventBy, eventInstant);
  }

  public SummaryCard toSummaryCard() {
    var summaryData = SummaryDataView.newBuilder();
    for (var detail : details.entrySet()) {
      summaryData.addStringValue(detail.getKey(), detail.getValue());
    }
    files.forEach(summaryData::addFileValue);
    summaryData
        .addStringValue(EVENT_BY_KEY, eventBy)
        .addStringValue(DATE_KEY, DateFormatUtil.convertToDisplayTextWithTime(eventInstant));

    return SummaryCard.simpleSummaryCardWithHeading(eventType.getDisplayName(), summaryData.build());
  }
}
