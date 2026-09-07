package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

public record RecordWorkProgrammeAmendmentSummaryView(
    String workProgrammeDescription,
    String dueDate,
    String decision,
    String amendedDuration,
    String amendedText
) {
}
