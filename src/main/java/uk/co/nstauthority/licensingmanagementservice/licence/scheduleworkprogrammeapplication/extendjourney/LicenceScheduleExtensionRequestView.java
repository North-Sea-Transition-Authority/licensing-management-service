package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;

public record LicenceScheduleExtensionRequestView(
    String id,
    String displayName,
    boolean isPhase,
    boolean isRequested,
    ThreeFieldDuration duration){}