package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

public record ScheduleState(
    LicenceScheduleTerm currentTerm,
    LicenceSchedulePhase currentPhase,
    LicenceScheduleTerm nextTerm,
    LicenceSchedulePhase nextPhase
) {}