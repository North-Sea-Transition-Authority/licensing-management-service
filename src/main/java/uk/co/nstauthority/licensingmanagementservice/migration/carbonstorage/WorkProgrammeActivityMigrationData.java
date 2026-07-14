package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import java.util.List;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;

record WorkProgrammeActivityMigrationData(
    List<WorkProgrammeActivity> activities,
    Map<WorkProgrammeActivity, ActivityStatusData> activityStatusData,
    Map<WorkProgrammeActivity, ActivityCommentData> activityCommentData
) {}
