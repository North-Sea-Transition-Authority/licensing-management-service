package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

/**
 * The {@code ACTIVE} detail of a schedule the schedule migration wrote: the entities the work programme is written onto,
 * and the same schedule as the mapper reads it.
 */
record ActiveScheduleDetail(
    LicenceScheduleDetail detail,
    List<LicenceScheduleTerm> terms,
    List<LicenceSchedulePhase> phases,
    MigratedSchedule schedule
) {

  ActiveScheduleDetail {
    terms = List.copyOf(terms);
    phases = List.copyOf(phases);
  }
}
