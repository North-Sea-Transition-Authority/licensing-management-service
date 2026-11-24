package uk.co.nstauthority.licensingmanagementservice.licence.application;

import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

public record ApplicationUpdateSummaryCardView(
    String updateReason,
    String updateDeadline
) {

  public static ApplicationUpdateSummaryCardView from(ApplicationDetail applicationDetail) {
    var deadline = applicationDetail.getUpdateDeadlineDate();
    return new ApplicationUpdateSummaryCardView(
        applicationDetail.getUpdateReason(),
        deadline != null ? DateFormatUtil.convertToDisplayText(deadline) : null
    );
  }
}
