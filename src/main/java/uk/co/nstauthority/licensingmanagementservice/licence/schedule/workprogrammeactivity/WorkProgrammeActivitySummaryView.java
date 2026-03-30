package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

public record WorkProgrammeActivitySummaryView(
    String category,
    String description,
    String commitment,
    String dueDate,
    String comments
) {

  public static WorkProgrammeActivitySummaryView fromWorkProgrammeActivity(WorkProgrammeActivity workProgrammeActivity) {
    var dueDateString = workProgrammeActivity.getDueDate() != null
        ? DateFormatUtil.convertToDisplayText(workProgrammeActivity.getDueDate())
        : "";

    return new WorkProgrammeActivitySummaryView(
        workProgrammeActivity.getCategoryString(),
        workProgrammeActivity.getDescription(),
        workProgrammeActivity.getCommitment().getDisplayName(),
        dueDateString,
        workProgrammeActivity.getComments()
    );
  }

}
