package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

public record RecordDurationChangeSummaryView(
    String displayName,
    boolean isPhase,
    String currentDuration,
    String currentEndDate,
    String change,
    String newDuration,
    String newEndDate
) {
}
