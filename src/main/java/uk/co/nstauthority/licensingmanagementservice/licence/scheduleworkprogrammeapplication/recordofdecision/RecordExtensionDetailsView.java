package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;

public record RecordExtensionDetailsView(
    String id,
    String displayName,
    String endDate,
    boolean isPhase,
    boolean isRequested,
    ThreeFieldDuration duration
){
}
