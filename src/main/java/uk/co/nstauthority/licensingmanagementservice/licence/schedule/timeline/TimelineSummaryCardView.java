package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.util.List;

public record TimelineSummaryCardView(
    String licenceStartDate,
    String licenceExpiryDate,
    boolean showRoundIssuedOn,
    String roundIssuedOn,
    String status,
    String licenceEndedDate,
    String finalTermEndDate,
    List<String> licenseeNames
) {
}
