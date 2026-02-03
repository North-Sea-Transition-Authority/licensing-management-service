package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

public record WorkProgrammeActivityView(
    String id,
    String dueDate,
    String category,
    String description,
    String categoryWithDueDate,
    String commitment
) {
}