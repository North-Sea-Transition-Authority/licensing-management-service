package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;

record DurationChangeCandidate(
    String id,
    String displayName,
    LocalDate startDate,
    LocalDate endDate,
    ThreeFieldDuration duration,
    boolean isPhase
) {
}
