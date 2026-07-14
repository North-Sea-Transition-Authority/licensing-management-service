package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import java.util.List;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

record ScheduleDetailsResult(
    List<LicenceScheduleDetail> details,
    Map<String, LicenceScheduleDetail> detailsByCaseDate
) {}
