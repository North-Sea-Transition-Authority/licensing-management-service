package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;

public record RecordReductionDetailsView(
    String id,
    String displayName,
    String endDate,
    boolean isPhase,
    boolean isSelected,
    ThreeFieldDuration duration
){
}
