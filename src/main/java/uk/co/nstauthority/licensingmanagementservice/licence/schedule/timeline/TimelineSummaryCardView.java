package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

public record TimelineSummaryCardView(
    String licenceStartDate,
    boolean showRoundIssuedOn,
    String roundIssuedOn,
    String status
) {
}
