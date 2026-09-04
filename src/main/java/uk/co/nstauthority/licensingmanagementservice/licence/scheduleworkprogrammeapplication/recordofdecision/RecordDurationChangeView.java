package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

public record RecordDurationChangeView(
    String id,
    String displayName,
    boolean isPhase,
    String currentEndDate,
    String currentDuration,
    boolean canReduce,
    boolean canExtend
) {
}
