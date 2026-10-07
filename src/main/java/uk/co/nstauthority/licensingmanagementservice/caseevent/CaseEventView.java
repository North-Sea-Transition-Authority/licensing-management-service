package uk.co.nstauthority.licensingmanagementservice.caseevent;

import java.time.Instant;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

public record CaseEventView(
    CaseEventType eventType,
    String eventBy,
    Instant eventInstant
) {

  static final String EVENT_BY_KEY = "Event by";
  static final String DATE_KEY = "Date";

  public SummaryCard toSummaryCard() {
    return SummaryCard.simpleSummaryCardWithHeading(
        eventType.getDisplayName(),
        SummaryDataView.newBuilder()
            .addStringValue(EVENT_BY_KEY, eventBy)
            .addStringValue(DATE_KEY, DateFormatUtil.convertToDisplayTextWithTime(eventInstant))
            .build()
    );
  }
}
