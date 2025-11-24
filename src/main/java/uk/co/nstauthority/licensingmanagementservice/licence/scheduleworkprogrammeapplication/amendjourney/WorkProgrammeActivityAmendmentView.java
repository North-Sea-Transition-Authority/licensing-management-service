package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

public record WorkProgrammeActivityAmendmentView(
    String id,
    String dueDate,
    String category,
    String description,
    String categoryWithDueDate
) {
}