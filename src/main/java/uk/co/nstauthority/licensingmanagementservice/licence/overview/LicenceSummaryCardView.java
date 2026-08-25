package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import java.util.List;

public record LicenceSummaryCardView(
    String status,
    List<String> licenseeNames,
    boolean showRoundIssuedOn,
    String roundIssuedOn
) {
}
