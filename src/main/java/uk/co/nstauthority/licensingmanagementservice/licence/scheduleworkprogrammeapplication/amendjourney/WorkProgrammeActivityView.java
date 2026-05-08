package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;

public record WorkProgrammeActivityView(
    String id,
    String dueDate,
    String category,
    String description,
    String categoryWithDueDate,
    String commitment,
    WorkProgrammeStatus status
) {
}