package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface RecordOfDecisionWorkProgrammeRepository
    extends JpaRepository<RecordOfDecisionWorkProgramme, UUID>, NotDuplicationSource {

  List<RecordOfDecisionWorkProgramme> findAllByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);

  Optional<RecordOfDecisionWorkProgramme> findByScheduleWorkProgrammeApplicationDetailAndWorkProgrammeActivity(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      WorkProgrammeActivity workProgrammeActivity);

  boolean existsByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);

  boolean existsByScheduleWorkProgrammeApplicationDetailAndWorkProgrammeActivityId(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      UUID workProgrammeActivityId);
}
