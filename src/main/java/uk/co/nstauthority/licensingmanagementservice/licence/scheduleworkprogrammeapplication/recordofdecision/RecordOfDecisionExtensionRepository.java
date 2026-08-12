package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface RecordOfDecisionExtensionRepository
    extends JpaRepository<RecordOfDecisionExtension, UUID>, NotDuplicationSource {

  List<RecordOfDecisionExtension> findAllByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);

  Optional<RecordOfDecisionExtension> findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      UUID phaseId);

  Optional<RecordOfDecisionExtension> findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      UUID termId);

  boolean existsByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);
}
