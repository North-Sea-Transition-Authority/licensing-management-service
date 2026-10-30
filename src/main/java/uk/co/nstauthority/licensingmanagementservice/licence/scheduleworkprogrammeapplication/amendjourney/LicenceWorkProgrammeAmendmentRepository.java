package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface LicenceWorkProgrammeAmendmentRepository
    extends JpaRepository<LicenceWorkProgrammeAmendmentRequest, UUID>, DuplicationSource<ScheduleWorkProgrammeApplicationDetail> {

  Optional<LicenceWorkProgrammeAmendmentRequest> findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivity(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails,
      WorkProgrammeActivity workProgrammeActivity
  );

  @DuplicateThisOnUpdate
  List<LicenceWorkProgrammeAmendmentRequest> findAllByScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails);

  boolean existsByScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails);

  boolean existsByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeCompletionDateChangeRequestedTrue(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  );

  boolean existsByWorkProgrammeActivityIdAndScheduleWorkProgrammeApplicationDetails(
      UUID workProgrammeActivityId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  );
}