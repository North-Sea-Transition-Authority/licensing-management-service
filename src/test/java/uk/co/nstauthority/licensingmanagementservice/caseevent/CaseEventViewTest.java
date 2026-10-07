package uk.co.nstauthority.licensingmanagementservice.caseevent;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

class CaseEventViewTest {

  @Test
  void toSummaryCard_whenNoDetails_thenShowsCompletedByAndDate() {
    var eventInstant = Instant.parse("2026-09-24T10:15:30Z");
    var view = new CaseEventView(CaseEventType.APPLICATION_SUBMITTED, Map.of(), "Jane Smith", eventInstant);

    var result = view.toSummaryCard();

    var expected = SummaryCard.simpleSummaryCardWithHeading(
        "Application submitted",
        SummaryDataView.newBuilder()
            .addStringValue(CaseEventView.EVENT_BY_KEY, "Jane Smith")
            .addStringValue(CaseEventView.DATE_KEY, DateFormatUtil.convertToDisplayTextWithTime(eventInstant))
            .build()
    );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void toSummaryCard_whenDetails_thenShowsDetailsBeforeCompletedByAndDate() {
    var eventInstant = Instant.parse("2026-09-24T10:15:30Z");
    var view = new CaseEventView(
        CaseEventType.STEWARD_ALLOCATED,
        Map.of(CaseEventService.ALLOCATED_TO_KEY, "John Steward"),
        "Jane Smith",
        eventInstant
    );

    var result = view.toSummaryCard();

    var expected = SummaryCard.simpleSummaryCardWithHeading(
        "Steward allocated",
        SummaryDataView.newBuilder()
            .addStringValue(CaseEventService.ALLOCATED_TO_KEY, "John Steward")
            .addStringValue(CaseEventView.EVENT_BY_KEY, "Jane Smith")
            .addStringValue(CaseEventView.DATE_KEY, DateFormatUtil.convertToDisplayTextWithTime(eventInstant))
            .build()
    );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
