package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;

public record RecordDurationChangeView(
    String id,
    String displayName,
    boolean isPhase,
    String currentEndDate,
    String currentDuration,
    ThreeFieldDuration duration,
    boolean canReduce,
    boolean canExtend
) {
}
