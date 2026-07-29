package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import java.time.Instant;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;

record MigratedActivity(
    WorkProgrammeActivity activity,
    String statusDisplayName,
    String comment,
    Instant caseInstant
) {}
