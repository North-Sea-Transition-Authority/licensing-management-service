package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.time.LocalDate;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;

record DurationChangeCandidate(
    String id,
    UUID originalEventId,
    String displayName,
    LocalDate startDate,
    LocalDate endDate,
    ThreeFieldDuration duration,
    boolean isPhase
) {
}
